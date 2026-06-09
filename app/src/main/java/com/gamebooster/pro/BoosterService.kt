package com.gamebooster.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.IOException
import kotlin.random.Random

class BoosterService : VpnService() {

    companion object {
        const val TAG = "BoosterService"
        const val ACTION_START = "com.gamebooster.pro.action.START"
        const val ACTION_STOP = "com.gamebooster.pro.action.STOP"
        const val BROADCAST_TELEMETRY = "com.gamebooster.pro.TELEMETRY"
        
        // Extras
        const val EXTRA_PING = "ping"
        const val EXTRA_TRAFFIC = "traffic"
        const val EXTRA_ACTIVE = "active"
        const val EXTRA_COUNTRY = "country"
        const val EXTRA_FLAG = "flag"

        private const val CHANNEL_ID = "BoosterChannel"
        private const val NOTIFICATION_ID = 4004
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var simulatedPackets = 0
    private var telemetryRunnable: Runnable? = null
    private var targetPackage: String? = null
    private var initialTxPackets: Long = 0
    private var initialRxPackets: Long = 0
    private var lastRealPing = 25
    private val pingExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    // Active Virtual Location info fields
    private var activeCountryName: String? = null
    private var activeFlagEmoji: String? = null
    private var activeDnsPrimary: String? = null
    private var activeDnsSecondary: String? = null
    private var activeIpAddress: String? = null
    private var activeGeoNode: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // CRITICAL CRASH PREVENTION: strict structural null-check on the intent object
        if (intent == null) {
            Log.d(TAG, "onStartCommand: Intent is null, returning START_STICKY")
            return START_STICKY
        }

        val action = intent.action
        Log.d(TAG, "onStartCommand Action: $action")

        when (action) {
            ACTION_START -> {
                val selectedPackage = intent.getStringExtra("selected_game_package")
                val countryName = intent.getStringExtra("selected_country_name")
                val flagEmoji = intent.getStringExtra("selected_flag_emoji")
                val dnsPrimary = intent.getStringExtra("selected_dns_primary")
                val dnsSecondary = intent.getStringExtra("selected_dns_secondary")
                val ipAddress = intent.getStringExtra("selected_ip_address")
                val geoNode = intent.getStringExtra("selected_geo_node")

                startBooster(
                    selectedPackage,
                    countryName,
                    flagEmoji,
                    dnsPrimary,
                    dnsSecondary,
                    ipAddress,
                    geoNode
                )
            }
            ACTION_STOP -> {
                stopBooster()
            }
        }
        return START_STICKY
    }

    private fun startBooster(
        selectedPackage: String?,
        countryName: String?,
        flagEmoji: String?,
        dnsPrimary: String?,
        dnsSecondary: String?,
        ipAddress: String?,
        geoNode: String?
    ) {
        if (isRunning) return
        isRunning = true

        val targetApp = selectedPackage ?: "com.activision.callofduty.shooter"
        targetPackage = targetApp
        setupTrafficInitialCounters()

        // Assign active virtual location config parameters
        activeCountryName = countryName ?: "Singapore (SG)"
        activeFlagEmoji = flagEmoji ?: "🇸🇬"
        activeDnsPrimary = dnsPrimary ?: "165.21.83.88"
        activeDnsSecondary = dnsSecondary ?: "1.1.1.1"
        activeIpAddress = ipAddress ?: "101.100.200.5"
        activeGeoNode = geoNode ?: "sg-optimal.gamebooster.pro"

        startForeground(NOTIFICATION_ID, buildNotification(true))

        // Configure VpnService build tunnel
        try {
            val builder = Builder()
            builder.setSession("GameBoosterPro Session")
            builder.setMtu(1500)
            
            // Add custom local IP addresses for safe routing simulation
            val assignedVpnIp = activeIpAddress ?: "10.0.0.2"
            builder.addAddress(assignedVpnIp, 24)
            builder.addRoute("10.0.0.0", 24)

            // Dynamically set regional primary and secondary DNS to direct DNS geo-signatures
            try {
                builder.addDnsServer(activeDnsPrimary!!)
                builder.addDnsServer(activeDnsSecondary!!)
            } catch (e: Exception) {
                Log.w(TAG, "Failed adding dynamic DNS servers, using fallback", e)
                builder.addDnsServer("1.1.1.1")
            }

            // CRITICAL REQUIREMENT: Split-tunnel configuration for specific game app targets
            try {
                builder.addAllowedApplication(targetApp)
            } catch (e: Exception) {
                Log.w(TAG, "Could not add allowed application: $targetApp", e)
            }

            // CRITICAL REQUIREMENT: Attach allowBypass() so initial configurations can download normally
            builder.allowBypass()

            vpnInterface = builder.establish()
            Log.d(TAG, "VPN tunnel established: $vpnInterface")
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing VPN interface, using simulation backup", e)
        }

        startTelemetrySimulation()
    }

    private fun getTargetAppUid(): Int {
        val packageName = targetPackage ?: return -1
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.uid
        } catch (e: Exception) {
            -1
        }
    }

    private fun setupTrafficInitialCounters() {
        val uid = getTargetAppUid()
        if (uid != -1) {
            val tx = TrafficStats.getUidTxPackets(uid)
            val rx = TrafficStats.getUidRxPackets(uid)
            if (tx != TrafficStats.UNSUPPORTED.toLong()) {
                initialTxPackets = tx
                initialRxPackets = rx
                return
            }
        }
        // Fallback to custom device total
        initialTxPackets = TrafficStats.getTotalTxPackets()
        initialRxPackets = TrafficStats.getTotalRxPackets()
        if (initialTxPackets == TrafficStats.UNSUPPORTED.toLong()) {
            initialTxPackets = 0
            initialRxPackets = 0
        }
    }

    private fun stopBooster() {
        if (!isRunning) return
        isRunning = false

        stopTelemetrySimulation()

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: IOException) {
            Log.e(TAG, "Exception closing VPN interface", e)
        }

        stopForeground(true)
        stopSelf()

        // Send inactive broadcast update
        sendTelemetryBroadcast(0, "0 PKTS/S", false)
    }

    private fun measureRealPing() {
        pingExecutor.execute {
            if (!isRunning) return@execute
            val primaryTarget = activeDnsPrimary
            val secondaryTarget = activeDnsSecondary
            val targets = if (primaryTarget != null) {
                listOf(primaryTarget, secondaryTarget ?: "1.1.1.1", "8.8.8.8")
            } else {
                listOf("1.1.1.1", "8.8.8.8", "9.9.9.9")
            }
            var pingResult = -1
            for (target in targets) {
                val startTime = System.currentTimeMillis()
                try {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(target, 53), 1000)
                    socket.close()
                    pingResult = (System.currentTimeMillis() - startTime).toInt()
                    if (pingResult in 1..999) break
                } catch (e: Exception) {
                    // Try next target
                }
            }
            if (pingResult > 0) {
                lastRealPing = pingResult
            } else {
                lastRealPing = Random.nextInt(32, 45)
            }
        }
    }

    private fun startTelemetrySimulation() {
        telemetryRunnable = object : Runnable {
            override fun run() {
                if (!isRunning) return

                // Measure real RTT ping to an optimal server dynamically if active
                measureRealPing()
                val ping = lastRealPing
                
                val uid = getTargetAppUid()
                var txPackets: Long = 0
                var rxPackets: Long = 0
                if (uid != -1) {
                    txPackets = TrafficStats.getUidTxPackets(uid)
                    rxPackets = TrafficStats.getUidRxPackets(uid)
                    if (txPackets == TrafficStats.UNSUPPORTED.toLong()) {
                        txPackets = 0
                        rxPackets = 0
                    }
                } else {
                    txPackets = TrafficStats.getTotalTxPackets()
                    rxPackets = TrafficStats.getTotalRxPackets()
                    if (txPackets == TrafficStats.UNSUPPORTED.toLong()) {
                        txPackets = 0
                        rxPackets = 0
                    }
                }

                val currentTotal = txPackets + rxPackets
                val initialTotal = initialTxPackets + initialRxPackets
                val packetsUsed = if (currentTotal >= initialTotal && initialTotal > 0) {
                    currentTotal - initialTotal
                } else {
                    simulatedPackets += Random.nextInt(25, 75)
                    simulatedPackets.toLong()
                }

                val trafficText = "$packetsUsed PKTS"

                // Send immediate status update out
                sendTelemetryBroadcast(ping, trafficText, true)

                // Repeat transmission loop every second
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(telemetryRunnable!!)
    }

    private fun stopTelemetrySimulation() {
        telemetryRunnable?.let { handler.removeCallbacks(it) }
        telemetryRunnable = null
        simulatedPackets = 0
    }

    private fun sendTelemetryBroadcast(ping: Int, traffic: String, active: Boolean) {
        val intent = Intent(BROADCAST_TELEMETRY).apply {
            putExtra(EXTRA_PING, ping)
            putExtra(EXTRA_TRAFFIC, traffic)
            putExtra(EXTRA_ACTIVE, active)
            if (active) {
                putExtra(EXTRA_COUNTRY, activeCountryName ?: "")
                putExtra(EXTRA_FLAG, activeFlagEmoji ?: "")
            }
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(active: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val text = if (active) "Gaming Optimized Network Tunnel is ENGAGED" else "Network Optimizer Suspended"
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("GameBoosterPro Active")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("GameBoosterPro Active")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "GameBoosterPro Optimizer Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        stopBooster()
        try {
            pingExecutor.shutdown()
        } catch (e: Exception) {}
        super.onDestroy()
    }
}

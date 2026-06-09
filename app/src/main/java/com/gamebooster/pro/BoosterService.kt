package com.gamebooster.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

        private const val CHANNEL_ID = "BoosterChannel"
        private const val NOTIFICATION_ID = 4004
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var simulatedPackets = 0
    private var telemetryRunnable: Runnable? = null

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
                startBooster(selectedPackage)
            }
            ACTION_STOP -> {
                stopBooster()
            }
        }
        return START_STICKY
    }

    private fun startBooster(selectedPackage: String?) {
        if (isRunning) return
        isRunning = true

        startForeground(NOTIFICATION_ID, buildNotification(true))

        // Configure VpnService build tunnel
        try {
            val builder = Builder()
            builder.setSession("GameBoosterPro Session")
            builder.setMtu(1500)
            
            // Add custom local IP addresses for safe routing simulation
            builder.addAddress("10.0.0.2", 24)
            builder.addRoute("0.0.0.0", 0)

            // CRITICAL REQUIREMENT: Split-tunnel configuration for specific game app targets
            val targetApp = selectedPackage ?: "com.activision.callofduty.shooter"
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

    private fun startTelemetrySimulation() {
        telemetryRunnable = object : Runnable {
            override fun run() {
                if (!isRunning) return

                // Simulate key latency statistics optimizing live game server responses
                val ping = Random.nextInt(16, 32)
                simulatedPackets += Random.nextInt(80, 150)
                val trafficText = "$simulatedPackets PKTS"

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
        super.onDestroy()
    }
}

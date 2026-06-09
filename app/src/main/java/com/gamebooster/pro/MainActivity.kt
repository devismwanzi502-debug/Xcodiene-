package com.gamebooster.pro

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.gamebooster.pro.databinding.ActivityMainBinding

data class GameProfile(val name: String, val packageId: String, val logo: String)

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isBoosterActive = false
    private var currentProfileIndex = 0

    private val gameProfiles = listOf(
        GameProfile("Call of Duty: Mobile", "com.activision.callofduty.shooter", "COD"),
        GameProfile("eFootball", "jp.konami.pesam", "EFB")
    )

    companion object {
        private const val REQUEST_VPN = 1010
        private const val REQUEST_OVERLAY = 1020
    }

    private val telemetryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BoosterService.BROADCAST_TELEMETRY) {
                val ping = intent.getIntExtra(BoosterService.EXTRA_PING, 0)
                val traffic = intent.getStringExtra(BoosterService.EXTRA_TRAFFIC) ?: "0 PKTS"
                val active = intent.getBooleanExtra(BoosterService.EXTRA_ACTIVE, false)

                isBoosterActive = active
                updateUIState(ping, traffic, active)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdownMenu()
        setupClickListeners()
    }

    private fun setupDropdownMenu() {
        val namesList = gameProfiles.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, namesList)
        binding.selectGameProfile.adapter = adapter
        
        binding.selectGameProfile.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentProfileIndex = position
                val selectedProfile = gameProfiles[position]
                
                binding.textGameTitle.text = selectedProfile.name
                binding.textGameLogo.text = selectedProfile.logo
                
                binding.textTunnelStateBadge.text = "MEASURING NODE..."
                binding.textTunnelStateBadge.setTextColor(Color.parseColor("#1A73E8"))
                
                ServerPicker.findOptimalServer { optimalServer, _ ->
                    if (optimalServer != null) {
                        Toast.makeText(this@MainActivity, "Optimal Node: ${optimalServer.name} (${optimalServer.pingMs}ms)", Toast.LENGTH_SHORT).show()
                        if (!isBoosterActive) {
                            binding.textPing.text = optimalServer.pingMs.toString()
                            updateProgressIndicator(optimalServer.pingMs)
                        }
                        binding.textTunnelStateBadge.text = "BOUND TO ${optimalServer.name.uppercase()}"
                        binding.textTunnelStateBadge.setTextColor(Color.parseColor("#007A33"))
                    } else {
                        binding.textTunnelStateBadge.text = "OFFLINE"
                        binding.textTunnelStateBadge.setTextColor(Color.parseColor("#E53935"))
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        binding.btnToggleBooster.setOnClickListener {
            toggleBoosterTunnel()
        }

        binding.btnToggleFloatingBubble.setOnClickListener {
            toggleFloatingOverlay()
        }
    }

    private fun toggleBoosterTunnel() {
        if (isBoosterActive) {
            val stopIntent = Intent(this, BoosterService::class.java).apply {
                action = BoosterService.ACTION_STOP
            }
            startService(stopIntent)
            Toast.makeText(this, "Optimizing Engine Disengaged", Toast.LENGTH_SHORT).show()
        } else {
            val prepareIntent = VpnService.prepare(this)
            if (prepareIntent != null) {
                startActivityForResult(prepareIntent, REQUEST_VPN)
            } else {
                startBoosterServiceDirectly()
            }
        }
    }

    private fun startBoosterServiceDirectly() {
        val selectedProfile = gameProfiles[currentProfileIndex]
        val startIntent = Intent(this, BoosterService::class.java).apply {
            action = BoosterService.ACTION_START
            putExtra("selected_game_package", selectedProfile.packageId)
        }
        startService(startIntent)
        Toast.makeText(this, "Speed Corridor engaged!", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFloatingOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_OVERLAY)
            Toast.makeText(this, "Enable Draw Overlays permission for the bubble control", Toast.LENGTH_LONG).show()
        } else {
            val serviceIntent = Intent(this, FloatingBubbleService::class.java)
            if (FloatingBubbleService.isOverlayActive) {
                stopService(serviceIntent)
                Toast.makeText(this, "Drifting HUD Suspended", Toast.LENGTH_SHORT).show()
            } else {
                startService(serviceIntent)
                Toast.makeText(this, "Drifting HUD Spawned!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProgressIndicator(ping: Int) {
        val density = resources.displayMetrics.density
        val targetWidthPx = ((ping * 12).coerceIn(40, 240) * density).toInt()
        val params = binding.viewPingProgressBar.layoutParams
        params.width = targetWidthPx
        binding.viewPingProgressBar.layoutParams = params
    }

    private fun updateUIState(ping: Int, traffic: String, active: Boolean) {
        if (active) {
            binding.textPing.text = "$ping"
            binding.textTraffic.text = traffic
            binding.textStatusState.text = "ACTIVE"
            binding.textStatusState.setTextColor(Color.parseColor("#007A33"))
            binding.viewStatusIndicatorPill.setBackgroundColor(Color.parseColor("#007A33"))
            binding.viewSelectedGameStatusPill.setBackgroundColor(Color.parseColor("#007A33"))
            binding.btnToggleBooster.text = "SUSPEND ENGINE"
            binding.btnToggleBooster.setBackgroundResource(R.drawable.button_danger_bg)
            
            binding.textJitter.text = String.format("%.1f ms", 0.8 + (ping % 4) * 0.15)
            binding.textPacketLoss.text = "0.00 %"
            binding.textIntensity.text = if (ping < 22) "ULTRA-LOW" else "OPTIMIZED"
            
            updateProgressIndicator(ping)
        } else {
            binding.textPing.text = "0"
            binding.textTraffic.text = "0 PKTS/S"
            binding.textStatusState.text = "OFFLINE"
            binding.textStatusState.setTextColor(Color.parseColor("#E53935"))
            binding.viewStatusIndicatorPill.setBackgroundColor(Color.parseColor("#E53935"))
            binding.viewSelectedGameStatusPill.setBackgroundColor(Color.parseColor("#888888"))
            binding.btnToggleBooster.text = "ENGAGE ENGINE"
            binding.btnToggleBooster.setBackgroundResource(R.drawable.button_primary_bg)
            
            binding.textJitter.text = "1.2 ms"
            binding.textPacketLoss.text = "0.00 %"
            binding.textIntensity.text = "STANDBY"
            
            val density = resources.displayMetrics.density
            val params = binding.viewPingProgressBar.layoutParams
            params.width = (40 * density).toInt()
            binding.viewPingProgressBar.layoutParams = params
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN && resultCode == Activity.RESULT_OK) {
            startBoosterServiceDirectly()
        } else if (requestCode == REQUEST_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                toggleFloatingOverlay()
            } else {
                Toast.makeText(this, "Permission declined for Floating HUD Control.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(BoosterService.BROADCAST_TELEMETRY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(telemetryReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(telemetryReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(telemetryReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}

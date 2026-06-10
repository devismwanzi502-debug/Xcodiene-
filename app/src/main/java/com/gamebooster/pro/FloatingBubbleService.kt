package com.gamebooster.pro

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: FrameLayout? = null
    private var bubbleLayout: FrameLayout? = null
    private var panelLayout: LinearLayout? = null
    
    private var tvPanelPing: TextView? = null
    private var tvPanelLocation: TextView? = null
    private var btnPanelToggle: Button? = null
    private var statusPill: View? = null

    private var isBoosterActive = false

    companion object {
        var isOverlayActive = false
    }

    private val telemetryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BoosterService.BROADCAST_TELEMETRY) {
                val ping = intent.getIntExtra(BoosterService.EXTRA_PING, 0)
                val active = intent.getBooleanExtra(BoosterService.EXTRA_ACTIVE, false)
                val country = intent.getStringExtra(BoosterService.EXTRA_COUNTRY) ?: ""
                val flag = intent.getStringExtra(BoosterService.EXTRA_FLAG) ?: ""
                isBoosterActive = active
                
                tvPanelPing?.text = if (active) "PING: $ping ms" else "PING: STANDBY"
                tvPanelLocation?.text = if (active && country.isNotEmpty()) "V-LOC: $flag $country" else "LOC: STANDBY"
                btnPanelToggle?.text = if (active) "SUSPEND" else "ENGAGE"
                btnPanelToggle?.setBackgroundColor(if (active) Color.parseColor("#E53935") else Color.parseColor("#1A73E8"))
                statusPill?.setBackgroundColor(if (active) Color.parseColor("#007A33") else Color.parseColor("#E53935"))
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        
        // Prevent background manager exception or AppOps warnings if restarted without overlay control permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            android.util.Log.w("FloatingBubbleService", "Overlay permission not granted. Terminating background HUD service.")
            stopSelf()
            return
        }

        isOverlayActive = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val parentLayout = FrameLayout(this)
        floatingView = parentLayout

        // 1. COLLAPSED BUBBLE LAYOUT
        val bubble = FrameLayout(this).apply {
            val sizePx = dpToPx(56)
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor("#121212"))
                setStroke(dpToPx(2), Color.parseColor("#1A73E8"))
            }
        }
        val bubbleText = TextView(this).apply {
            text = "⚡\nGBP"
            setTextColor(Color.parseColor("#1A73E8"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        bubble.addView(bubbleText)
        bubbleLayout = bubble

        // 2. EXPANDED PANEL LAYOUT
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            elevation = 10f
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(dpToPx(160), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#1A1C1E"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#2E3033"))
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val pill = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), dpToPx(6)).apply {
                setMargins(0, 0, dpToPx(6), 0)
            }
            setBackgroundColor(Color.parseColor("#E53935"))
        }
        statusPill = pill
        
        val titleText = TextView(this).apply {
            text = "OPTIMIZER PANEL"
            setTextColor(Color.parseColor("#888888"))
            textSize = 8f
            typeface = Typeface.DEFAULT_BOLD
        }
        headerRow.addView(pill)
        headerRow.addView(titleText)

        val pingTv = TextView(this).apply {
            text = "PING: STANDBY"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dpToPx(6), 0, dpToPx(4))
        }
        tvPanelPing = pingTv

        val locationTv = TextView(this).apply {
            text = "LOC: STANDBY"
            setTextColor(Color.parseColor("#888888"))
            textSize = 9f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, dpToPx(8))
        }
        tvPanelLocation = locationTv

        val toggleBtn = Button(this).apply {
            text = "ENGAGE"
            setTextColor(Color.WHITE)
            textSize = 9f
            setBackgroundColor(Color.parseColor("#1A73E8"))
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(32))
            setOnClickListener {
                val intent = Intent(this@FloatingBubbleService, BoosterService::class.java).apply {
                    action = if (isBoosterActive) BoosterService.ACTION_STOP else BoosterService.ACTION_START
                }
                startService(intent)
                Toast.makeText(this@FloatingBubbleService, if (isBoosterActive) "Boost Engine Suspended" else "Boost Engine Active", Toast.LENGTH_SHORT).show()
                toggleViewState()
            }
        }
        btnPanelToggle = toggleBtn

        panel.addView(headerRow)
        panel.addView(pingTv)
        panel.addView(locationTv)
        panel.addView(toggleBtn)
        panelLayout = panel

        parentLayout.addView(bubble)
        parentLayout.addView(panel)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        parentLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var clickThreshold = 10

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < clickThreshold && diffY < clickThreshold) {
                            toggleViewState()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not initialize floating overlay", Toast.LENGTH_SHORT).show()
            stopSelf()
        }

        try {
            val filter = IntentFilter(BoosterService.BROADCAST_TELEMETRY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    registerReceiver(telemetryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } catch (e: SecurityException) {
                    registerReceiver(telemetryReceiver, filter, Context.RECEIVER_EXPORTED)
                }
            } else {
                registerReceiver(telemetryReceiver, filter)
            }
        } catch (e: Throwable) {
            android.util.Log.e("FloatingBubbleService", "Failed to register telemetry receiver", e)
        }
    }

    private fun toggleViewState() {
        if (bubbleLayout?.visibility == View.VISIBLE) {
            bubbleLayout?.visibility = View.GONE
            panelLayout?.visibility = View.VISIBLE
        } else {
            panelLayout?.visibility = View.GONE
            bubbleLayout?.visibility = View.VISIBLE
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroy() {
        isOverlayActive = false
        try {
            unregisterReceiver(telemetryReceiver)
        } catch (e: Exception) {}
        
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        super.onDestroy()
    }
}

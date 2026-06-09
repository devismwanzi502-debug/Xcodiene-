package com.gamebooster.pro

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews

// 1. Ping Monitor Widget
class PingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_ping)
        views.setTextViewText(R.id.tvWidgetPing, "STANDBY")
        views.setTextColor(R.id.tvWidgetPing, Color.parseColor("#888888"))
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BoosterService.BROADCAST_TELEMETRY) {
            val ping = intent.getIntExtra(BoosterService.EXTRA_PING, 0)
            val active = intent.getBooleanExtra(BoosterService.EXTRA_ACTIVE, false)
            val text = if (active) "$ping ms" else "STANDBY"
            val color = if (active) "#1A73E8" else "#888888"

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PingWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            val views = RemoteViews(context.packageName, R.layout.widget_ping)
            views.setTextViewText(R.id.tvWidgetPing, text)
            views.setTextColor(R.id.tvWidgetPing, Color.parseColor(color))

            for (widgetId in allWidgetIds) {
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}

// 2. Traffic Counter Widget
class TrafficWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_traffic)
        views.setTextViewText(R.id.tvWidgetTraffic, "0 PKTS")
        views.setTextColor(R.id.tvWidgetTraffic, Color.parseColor("#888888"))
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BoosterService.BROADCAST_TELEMETRY) {
            val traffic = intent.getStringExtra(BoosterService.EXTRA_TRAFFIC) ?: "0 PKTS"
            val active = intent.getBooleanExtra(BoosterService.EXTRA_ACTIVE, false)
            val text = if (active) traffic else "0 PKTS"
            val color = if (active) "#FFFFFF" else "#888888"

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TrafficWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            val views = RemoteViews(context.packageName, R.layout.widget_traffic)
            views.setTextViewText(R.id.tvWidgetTraffic, text)
            views.setTextColor(R.id.tvWidgetTraffic, Color.parseColor(color))

            for (widgetId in allWidgetIds) {
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}

// 3. Quick-Toggle Actions Widget
class ActionsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Action Start Intent
        val startIntent = Intent(context, BoosterService::class.java).apply {
            action = BoosterService.ACTION_START
        }
        val startPendingIntent = PendingIntent.getService(context, 100, startIntent, flags)

        // Action Stop Intent
        val stopIntent = Intent(context, BoosterService::class.java).apply {
            action = BoosterService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(context, 101, stopIntent, flags)

        val views = RemoteViews(context.packageName, R.layout.widget_actions).apply {
            setOnClickPendingIntent(R.id.btnWidgetStart, startPendingIntent)
            setOnClickPendingIntent(R.id.btnWidgetStop, stopPendingIntent)
        }

        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

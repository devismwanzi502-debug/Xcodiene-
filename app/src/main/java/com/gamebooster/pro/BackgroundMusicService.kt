package com.gamebooster.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

class BackgroundMusicService : Service() {

    companion object {
        const val TAG = "BackgroundMusicService"
        const val ACTION_PLAY = "com.gamebooster.pro.action.PLAY"
        const val ACTION_PAUSE = "com.gamebooster.pro.action.PAUSE"
        const val ACTION_SET_TRACK = "com.gamebooster.pro.action.SET_TRACK"
        
        const val BROADCAST_MUSIC_STATE = "com.gamebooster.pro.MUSIC_STATE"
        const val BROADCAST_MUSIC_VISUALIZER = "com.gamebooster.pro.MUSIC_VISUALIZER"
        
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_TRACK_ID = "track_id"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_AMPLITUDE = "amplitude"

        private const val CHANNEL_ID = "MusicPlaybackChannel"
        private const val NOTIFICATION_ID = 5005
        
        val mellyEngine = MellySynthEngine()
        var isPlaying = false
        var currentTrackId = MellySynthEngine.TRACK_MURDER_ON_MY_MIND
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        mellyEngine.progressCallback = { seconds ->
            val intent = Intent(BROADCAST_MUSIC_STATE).apply {
                putExtra(EXTRA_IS_PLAYING, true)
                putExtra(EXTRA_TRACK_ID, currentTrackId)
                putExtra(EXTRA_PROGRESS, seconds)
            }
            sendBroadcast(intent)
        }

        mellyEngine.visualizerCallback = { amplitude ->
            val intent = Intent(BROADCAST_MUSIC_VISUALIZER).apply {
                putExtra(EXTRA_AMPLITUDE, amplitude)
            }
            sendBroadcast(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_PLAY -> {
                    val trackId = intent.getIntExtra(EXTRA_TRACK_ID, currentTrackId)
                    playTrack(trackId)
                }
                ACTION_PAUSE -> {
                    pauseTrack()
                }
                ACTION_SET_TRACK -> {
                    val trackId = intent.getIntExtra(EXTRA_TRACK_ID, currentTrackId)
                    currentTrackId = trackId
                    if (isPlaying) {
                        playTrack(trackId)
                    } else {
                        mellyEngine.setTrack(trackId)
                        broadcastStaticState()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun playTrack(trackId: Int) {
        currentTrackId = trackId
        isPlaying = true
        mellyEngine.start(trackId)
        
        startForegroundService()
    }

    private fun pauseTrack() {
        isPlaying = false
        mellyEngine.stop()
        stopForeground(true)
        broadcastStaticState()
    }

    private fun broadcastStaticState() {
        val intent = Intent(BROADCAST_MUSIC_STATE).apply {
            putExtra(EXTRA_IS_PLAYING, isPlaying)
            putExtra(EXTRA_TRACK_ID, currentTrackId)
            putExtra(EXTRA_PROGRESS, 0)
        }
        sendBroadcast(intent)
    }

    private fun startForegroundService() {
        val title = if (currentTrackId == MellySynthEngine.TRACK_MURDER_ON_MY_MIND) "Murder On My Mind" else "223s"
        val subtitle = "Synthesizing ultra-low latency background melody..."
        
        val notification = buildNotification(title, subtitle)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed startForeground for MusicService", e)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            99,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("SYS CHIP TUNE: $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Acoustic Background Stream"
            val descriptionText = "Ensures background music synthesis for focus acceleration"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mellyEngine.stop()
        super.onDestroy()
    }
}

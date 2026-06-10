package com.gamebooster.pro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlin.math.sin

class MellySynthEngine {
    companion object {
        private const val TAG = "MellySynthEngine"
        private const val SAMPLE_RATE = 22050
        
        const val TRACK_MURDER_ON_MY_MIND = 0
        const val TRACK_223S = 1
    }

    data class Note(val freq: Float, val durationMs: Int)

    private var audioTrack: AudioTrack? = null
    private var synthThread: Thread? = null
    private var isPlaying = false
    private var currentTrackId = TRACK_MURDER_ON_MY_MIND
    
    // Listener to feed frequency amplitude back to the visualizer bars
    var visualizerCallback: ((amplitude: Float) -> Unit)? = null
    var progressCallback: ((seconds: Int) -> Unit)? = null

    // Track 1: Murder On My Mind - Classic Melancholic Piano Arpeggios (Bb minor, Gb major, Db major, Ab major)
    private val murderNotes = listOf(
        // Bb Minor
        Note(466.16f, 350), Note(554.37f, 350), Note(698.46f, 350), Note(932.33f, 500), Note(698.46f, 350), Note(554.37f, 350),
        // Gb Major
        Note(369.99f, 350), Note(466.16f, 350), Note(554.37f, 350), Note(739.99f, 500), Note(554.37f, 350), Note(466.16f, 350),
        // Db Major
        Note(277.18f, 350), Note(349.23f, 350), Note(415.30f, 350), Note(554.37f, 500), Note(415.30f, 350), Note(349.23f, 350),
        // Ab Major
        Note(415.30f, 350), Note(523.25f, 350), Note(622.25f, 350), Note(830.61f, 500), Note(622.25f, 350), Note(523.25f, 350)
    )

    // Track 2: 223s - Plucky up-tempo minor trap lead (G# minor, bouncy rhythms)
    private val bouncyNotes = listOf(
        Note(415.30f, 200), Note(415.30f, 200), Note(0f, 100), Note(466.16f, 200), Note(493.88f, 300),
        Note(415.30f, 200), Note(554.37f, 200), Note(0f, 100), Note(493.88f, 200), Note(466.16f, 300),
        Note(415.30f, 200), Note(415.30f, 200), Note(0f, 100), Note(466.16f, 200), Note(493.88f, 300),
        Note(622.25f, 350), Note(554.37f, 350), Note(493.88f, 350), Note(466.16f, 350)
    )

    fun start(trackId: Int) {
        if (isPlaying) {
            stop()
        }
        currentTrackId = trackId
        isPlaying = true
        
        synthThread = Thread {
            runSynthLoop()
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isPlaying = false
        synthThread?.interrupt()
        synthThread = null
        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    stop()
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio track", e)
        }
        audioTrack = null
    }

    fun setTrack(trackId: Int) {
        if (isPlaying) {
            start(trackId)
        } else {
            currentTrackId = trackId
        }
    }

    private fun runSynthLoop() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        try {
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack play failed", e)
            return
        }

        val notesList = if (currentTrackId == TRACK_MURDER_ON_MY_MIND) murderNotes else bouncyNotes
        var noteIdx = 0
        var elapsedSeconds = 0

        while (isPlaying) {
            if (Thread.currentThread().isInterrupted) break
            
            val note = notesList[noteIdx]
            playSynthNote(note.freq, note.durationMs)
            
            elapsedSeconds += (note.durationMs / 1000f).toInt().coerceAtLeast(1)
            progressCallback?.invoke(elapsedSeconds % 180) // loops back relative to mock duration

            noteIdx = (noteIdx + 1) % notesList.size
        }
    }

    private fun playSynthNote(freq: Float, durationMs: Int) {
        val totalLength = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        if (totalLength <= 0) return
        
        val shortBuffer = ShortArray(totalLength)

        // Synthesize standard rich sine and organ-like harmonics for a professional piano hook
        val twoPi = 2.0 * Math.PI
        val fundamentalRad = twoPi * freq / SAMPLE_RATE
        val secondHarmonicRad = twoPi * (freq * 2.0) / SAMPLE_RATE
        val thirdHarmonicRad = twoPi * (freq * 3.0) / SAMPLE_RATE

        for (i in 0 until totalLength) {
            if (!isPlaying) return
            
            // Generate basic envelope (Attack, Decay, Release) to resemble sound of realistic bells/pianos
            val progress = i.toFloat() / totalLength
            val envelope = if (progress < 0.1f) {
                progress / 0.1f // Attack
            } else {
                1.0f - (progress - 0.1f) * 1.11f // Decay & long ring-out
            }.coerceIn(0f, 1f)

            if (freq <= 0f) {
                // Pause note/rest
                shortBuffer[i] = 0
            } else {
                val wave = sin(fundamentalRad * i) +
                           0.4 * sin(secondHarmonicRad * i) +
                           0.15 * sin(thirdHarmonicRad * i)
                
                shortBuffer[i] = (wave * 8000.0 * envelope).toInt().toShort()
            }
        }

        // Write short buffer samples to audio track
        try {
            audioTrack?.write(shortBuffer, 0, totalLength)
            
            // Pulse visualizer callback with calculated RMS amplitude
            if (freq > 0f) {
                visualizerCallback?.invoke(0.4f + (freq % 100) / 150f)
            } else {
                visualizerCallback?.invoke(0.05f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack write failed", e)
        }
    }
}

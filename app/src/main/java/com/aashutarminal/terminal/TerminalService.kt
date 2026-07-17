package com.aashutarminal.terminal

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aashutarminal.R

/**
 * Foreground service that keeps terminal sessions alive when the app is
 * backgrounded (similar to how Termux keeps shells running).
 */
class TerminalService : Service() {

    private val sessions = mutableListOf<TerminalSession>()
    private val binder = LocalBinder()

    inner class LocalBinder : android.os.Binder() {
        fun getService(): TerminalService = this@TerminalService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
    }

    fun addSession(session: TerminalSession) = sessions.add(session)
    fun removeSession(session: TerminalSession) = sessions.remove(session)

    private fun buildNotification(): Notification {
        val channelId = "aashu_terminal_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Terminal sessions", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Aashu Tarminal")
            .setContentText("${sessions.size} session(s) running")
            .setSmallIcon(R.drawable.ic_terminal)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        sessions.forEach { it.close() }
        sessions.clear()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1001
    }
}

package cu.todus.android.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import cu.todus.android.MainActivity
import cu.todus.android.R
import cu.todus.android.TodusApp
import cu.todus.android.data.SessionStore
import cu.todus.android.protocol.xmpp.XmppSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps the XMPP session alive while the app is in the background.
 * Started after a successful login; stops on logout.
 *
 * Per guía.txt §17, the server only allows ONE session per JID — if a second service instance
 * were to start it would kill the first. The service is a singleton via Hilt-scoped XmppSession.
 */
@AndroidEntryPoint
class XmppService : LifecycleService() {

    @Inject lateinit var xmpp: XmppSession
    @Inject lateinit var session: SessionStore

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification(getString(R.string.service_notification_text)))
        lifecycleScope.launch {
            val snap = session.snapshot()
            val phone = snap.phone ?: return@launch
            val token = snap.token ?: return@launch
            xmpp.connect(phone, token)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        xmpp.disconnect()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val contentPi = PendingIntent.getActivity(this, 0, launchIntent, flags)
        return NotificationCompat.Builder(this, TodusApp.CHANNEL_XMPP)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_todus_logo)
            .setOngoing(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIF_ID = 42
        fun start(context: Context) {
            val intent = Intent(context, XmppService::class.java)
            ServiceCompat.startForegroundService(context, intent)
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, XmppService::class.java))
        }
    }
}

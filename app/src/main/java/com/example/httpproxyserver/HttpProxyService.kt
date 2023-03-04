package com.example.httpproxyserver

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat

class HttpProxyService : Service() {

    private var port: Int = 1081
    private var httpProxy: HttpProxy? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        val notification = createForegroundNotification()
        startForeground(1, notification)
    }

    private fun createForegroundNotification(): Notification {
        val channelId = "$packageName.channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Http proxy notification channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Http proxy server")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startHttpProxy(intent)
        return START_NOT_STICKY
    }

    private fun startHttpProxy(intent: Intent?) {
        val port = intent?.getIntExtra("port", 1081) ?: 1081
        if (httpProxy != null) {
            if (port != this.port) {
                httpProxy?.stop()
            } else if (httpProxy!!.started) {
                return
            }
        }
        this.port = port
        httpProxy = HttpProxy.start(port)
        httpProxy!!.messageListener = {
            handler.post {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        httpProxy?.stop()
        httpProxy = null
    }

    companion object {
        fun newStartIntent(context: Context, port: Int): Intent {
            val intent = Intent(context, HttpProxyService::class.java)
            intent.putExtra("port", port)
            return intent
        }

        fun newStopIntent(context: Context): Intent {
            return Intent(context, HttpProxyService::class.java)
        }
    }
}


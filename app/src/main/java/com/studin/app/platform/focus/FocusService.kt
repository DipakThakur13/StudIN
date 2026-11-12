package com.studin.app.platform.focus

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat

/** STUB – replaced by the focus feature implementation. */
class FocusService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, FocusService::class.java))
        }
    }
}

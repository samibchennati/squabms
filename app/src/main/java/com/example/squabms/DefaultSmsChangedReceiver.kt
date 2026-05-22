package com.example.squabms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi

class DefaultSmsChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            handleDefaultSmsChanged(context, intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun handleDefaultSmsChanged(context: Context, intent: Intent) {
        // Check if this app is now the default SMS app
        val isDefaultSmsApp = intent.getBooleanExtra(
            Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP,
            false
        )

        if (isDefaultSmsApp) {
            onBecomeDefaultSmsApp(context)
        } else {
            onLoseDefaultSmsApp(context)
        }
    }

    private fun onBecomeDefaultSmsApp(context: Context) {

        Log.d("DefaultSmsChanged", "App is now the default SMS handler")
    }

    private fun onLoseDefaultSmsApp(context: Context) {


        Log.d("DefaultSmsChanged", "App is no longer the default SMS handler")
    }
}
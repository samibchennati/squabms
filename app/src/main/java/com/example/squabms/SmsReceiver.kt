package com.example.squabms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (message in messages) {
                val sender = message.originatingAddress
                val body = message.messageBody
                val timestamp = message.timestampMillis

                Log.d("SmsReceiver", "SMS from $sender: $body")



                showNotification(context, sender, body)
            }

            abortBroadcast()
        }
    }

    private fun showNotification(context: Context?, sender: String?, body: String?) {
        if (context == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("SmsReceiver", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val notification = NotificationCompat.Builder(context, "sms_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Message from $sender")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e("SmsReceiver", "Failed to post notification: ${e.message}")
        }
    }
}
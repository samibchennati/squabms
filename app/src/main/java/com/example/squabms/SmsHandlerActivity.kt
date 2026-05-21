package com.example.squabms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

class SmsHandlerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.action
        val data = intent.data

        if (action == Intent.ACTION_SENDTO && data != null && data.scheme == "sms") {
            val phoneNumber = data.schemeSpecificPart
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra("PHONE_NUMBER", phoneNumber)
            startActivity(intent)
        }

        finish()
    }
}
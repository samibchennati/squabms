package com.example.squabms

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class RespondViaMessageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.action

        if (action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            val phoneNumber = intent.data?.schemeSpecificPart ?: return

            val composeIntent = Intent(this, ConversationActivity::class.java).apply {
                putExtra("PHONE_NUMBER", phoneNumber) // Fixed key
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(composeIntent)
            finish()
        }
    }
}
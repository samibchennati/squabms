package com.example.squabms

import android.app.Activity
import android.os.Bundle

// This is a placeholder required for the Default SMS Handler manifest declaration.

class SmsReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
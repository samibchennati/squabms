package com.example.squabms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.app.Activity.RESULT_OK
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        resultCode = RESULT_OK
    }
}
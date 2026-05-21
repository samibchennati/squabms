package com.example.squabms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var rvConversations: RecyclerView
    private lateinit var adapter: ConversationAdapter
    private val conversationList = mutableListOf<Conversation>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadConversations()
        } else {
            Toast.makeText(this, "Permissions required to view messages.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvConversations = findViewById(R.id.rvConversations)

        adapter = ConversationAdapter(conversationList) { phoneNumber ->
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra("PHONE_NUMBER", phoneNumber)
            startActivity(intent)
        }

        rvConversations.layoutManager = LinearLayoutManager(this)
        rvConversations.adapter = adapter

        val swipeCallback = SwipeToDeleteCallback(adapter, contentResolver, conversationList)
        ItemTouchHelper(swipeCallback).attachToRecyclerView(rvConversations)

        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            loadConversations()
        }
    }

    private fun loadConversations() {
        try {
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null,
                null,
                "date DESC"
            )

            val conversationMap = mutableMapOf<String, Conversation>()

            cursor?.use {
                while (it.moveToNext()) {
                    val phoneNumber = it.getString(it.getColumnIndexOrThrow("address"))

                    if (phoneNumber.isNullOrEmpty()) continue

                    val lastMessage = it.getString(it.getColumnIndexOrThrow("body"))
                    val timestamp = it.getLong(it.getColumnIndexOrThrow("date"))

                    if (!conversationMap.containsKey(phoneNumber)) {
                        val conversation = Conversation(
                            contactName = getContactName(phoneNumber),
                            lastMessage = lastMessage ?: "",
                            timestamp = formatTime(timestamp),
                            avatarResId = R.drawable.ic_contact,
                            timestampLong = timestamp,
                            phoneNumber = phoneNumber
                        )
                        conversationMap[phoneNumber] = conversation
                    }
                }
            }

            conversationList.clear()
            conversationList.addAll(conversationMap.values.sortedByDescending { it.timestampLong })
            adapter.notifyDataSetChanged()

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading messages", Toast.LENGTH_LONG).show()
        }
    }

    private fun getContactName(phoneNumber: String): String {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )

            var contactName = phoneNumber
            cursor?.use {
                if (it.moveToFirst()) {
                    contactName = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                }
            }
            contactName
        } catch (e: Exception) {
            phoneNumber
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
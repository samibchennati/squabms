package com.example.squabms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var rvConversations: RecyclerView
    private lateinit var adapter: ConversationAdapter
    private val conversationList = mutableListOf<Conversation>()

    private val smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (uri?.path?.contains("sms") == true) {
                loadConversations()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadConversations()
        } else {
            Toast.makeText(this, "Permissions required to view messages.", Toast.LENGTH_LONG).show()
        }
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { contactUri ->
                val phoneNumber = getPhoneNumberFromContact(contactUri)
                if (phoneNumber != null) {
                    startConversation(phoneNumber)
                } else {
                    Toast.makeText(this, "Contact has no phone number.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        rvConversations = findViewById(R.id.rvConversations)

        adapter = ConversationAdapter(conversationList, this) { phoneNumber ->
            startConversation(phoneNumber)
        }

        rvConversations.layoutManager = LinearLayoutManager(this)
        rvConversations.adapter = adapter

        ItemTouchHelper(SwipeToDeleteCallback(adapter, contentResolver, conversationList))
            .attachToRecyclerView(rvConversations)

        findViewById<ImageView>(R.id.ivAddContact).setOnClickListener {
            contactPickerLauncher.launch(
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            )
        }

        contentResolver.registerContentObserver(
            Uri.parse("content://sms"),
            true,
            smsObserver
        )

        requestPermissionsIfNeeded()
        handleIncomingSmsIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(smsObserver)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sms_channel",
                "SMS Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val neededPermissions = mutableListOf<String>().apply {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_SMS)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.SEND_SMS)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_CONTACTS)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECEIVE_SMS)
            }
        }

        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions.toTypedArray())
        } else {
            loadConversations()
        }
    }

    private fun handleIncomingSmsIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SENDTO -> {
                val phoneNumber = intent.data?.schemeSpecificPart ?: return
                val message = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                startConversation(phoneNumber, message)
            }
        }
    }

    private fun loadConversations() {
        try {
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null, null, "date DESC"
            )

            val conversationMap = mutableMapOf<String, Conversation>()

            cursor?.use {
                while (it.moveToNext()) {
                    val phoneNumber = it.getString(it.getColumnIndexOrThrow("address"))
                    if (phoneNumber.isNullOrEmpty()) continue

                    val lastMessage = it.getString(it.getColumnIndexOrThrow("body")) ?: ""
                    val timestamp = it.getLong(it.getColumnIndexOrThrow("date"))

                    if (!conversationMap.containsKey(phoneNumber)) {
                        conversationMap[phoneNumber] = Conversation(
                            contactName = getContactName(phoneNumber),
                            lastMessage = lastMessage,
                            timestamp = formatTime(timestamp),
                            avatarResId = R.drawable.ic_contact,
                            timestampLong = timestamp,
                            phoneNumber = phoneNumber,
                            contactId = getContactId(phoneNumber)
                        )
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
            val cursor = contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
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

    private fun getContactId(phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            var contactId: String? = null
            cursor?.use {
                if (it.moveToFirst()) {
                    contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                }
            }
            contactId
        } catch (e: Exception) {
            null
        }
    }

    private fun getPhoneNumberFromContact(contactUri: Uri): String? {
        val contactId = contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts._ID), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)) else null
        } ?: return null

        return contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) else null
        }
    }

    private fun formatTime(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayDate = Calendar.getInstance()

        return when {
            messageDate.get(Calendar.YEAR) == todayDate.get(Calendar.YEAR) &&
                    messageDate.get(Calendar.DAY_OF_YEAR) == todayDate.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    private fun startConversation(phoneNumber: String, message: String = "") {
        startActivity(Intent(this, ConversationActivity::class.java).apply {
            putExtra("PHONE_NUMBER", phoneNumber)
            if (message.isNotEmpty()) putExtra("INITIAL_MESSAGE", message)
        })
    }
}
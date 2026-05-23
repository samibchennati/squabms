package com.example.squabms

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ConversationActivity : ComponentActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var tvContactName: TextView
    private lateinit var tvSendOnLabel: TextView
    private lateinit var tvSendOnDigits: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private val messageList = mutableListOf<Message>()
    private val itemList = mutableListOf<MessageItem>()
    private lateinit var messageAdapter: MessageAdapter
    private var phoneNumber = ""
    private var currentSimSlot = 0

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadMessages(phoneNumber)
        } else {
            Toast.makeText(this, "Permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_conversation)

        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: run {
            Toast.makeText(this, "No phone number found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvContactName = findViewById(R.id.tvContactName)
        tvSendOnLabel = findViewById(R.id.tvSendOnLabel)
        tvSendOnDigits = findViewById(R.id.tvSendOnDigits)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        tvContactName.text = getContactName(phoneNumber)
        updateSendOnDigits()
        tvSendOnDigits.setOnClickListener { showSimSelectionDialog() }

        messageAdapter = MessageAdapter(itemList, this, phoneNumber)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = messageAdapter

        val permissions = listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        ).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
            return
        }

        loadMessages(phoneNumber)

        rvMessages.post {
            if (itemList.isNotEmpty()) {
                rvMessages.scrollToPosition(itemList.size - 1)
            }
        }

        btnSend.setOnClickListener { sendMessage() }
    }

    private fun getMyPhoneNumberLastFour(): String {
        return try {
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)
            val activeSubscriptionInfo = subscriptionManager.activeSubscriptionInfoList?.find { it.simSlotIndex == currentSimSlot }

            val number = activeSubscriptionInfo?.number ?: telephonyManager.line1Number
            number?.takeLast(4) ?: "0000"
        } catch (e: Exception) {
            "0000"
        }
    }

    private fun updateSendOnDigits() {
        tvSendOnDigits.text = getMyPhoneNumberLastFour()
    }

    private fun showSimSelectionDialog() {
        try {
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList

            if (activeSubscriptions.isNullOrEmpty()) {
                Toast.makeText(this, "No SIM cards detected", Toast.LENGTH_SHORT).show()
                return
            }

            val simNames = mutableListOf<String>()
            val simIndices = mutableListOf<Int>()

            activeSubscriptions.forEachIndexed { index, sub ->
                val name = when (val displayNameObj = sub.displayName) {
                    is String -> if (displayNameObj.isEmpty()) "SIM ${sub.simSlotIndex + 1}" else displayNameObj
                    is Int -> try {
                        val resolved = getString(displayNameObj)
                        if (resolved.isEmpty()) "SIM ${sub.simSlotIndex + 1}" else resolved
                    } catch (e: Exception) {
                        "SIM ${sub.simSlotIndex + 1}"
                    }
                    else -> "SIM ${sub.simSlotIndex + 1}"
                }

                val digits = sub.number?.takeLast(4) ?: "Unknown"
                val finalEntry = "$name ($digits)"

                if (finalEntry.isNotBlank()) {
                    simNames.add(finalEntry)
                    simIndices.add(index)
                }
            }

            if (simNames.isEmpty()) {
                Toast.makeText(this, "No valid SIM data found", Toast.LENGTH_SHORT).show()
                return
            }

            val inflater = LayoutInflater.from(this)
            val popupView = inflater.inflate(R.layout.popup_sim_list, null)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            popupWindow.elevation = 10f
            popupWindow.isFocusable = true
            popupWindow.isOutsideTouchable = true

            val listView = popupView.findViewById<ListView>(R.id.lvSimList)
            popupView.findViewById<TextView>(R.id.tvDialogTitle).text = "Select SIM"

            listView.adapter = ArrayAdapter(this, R.layout.item_sim_option, simNames)

            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedSim = activeSubscriptions[simIndices[position]]
                currentSimSlot = selectedSim.simSlotIndex
                updateSendOnDigits()
                popupWindow.dismiss()
            }

            popupWindow.showAsDropDown(tvSendOnDigits, 0, 0)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading SIMs: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission missing", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, messageText, null, null)

            val values = ContentValues().apply {
                put("address", phoneNumber)
                put("body", messageText)
                put("date", System.currentTimeMillis())
                put("type", 2)
            }
            contentResolver.insert(Uri.parse("content://sms/sent"), values)

            etMessage.text.clear()

            val message = Message(
                body = messageText,
                timestamp = formatTime(System.currentTimeMillis()),
                isReceived = false,
                dateLong = System.currentTimeMillis()
            )
            messageList.add(message)

            val messageDate = getMessageDate(message.dateLong)
            if (itemList.isEmpty() || (itemList.lastOrNull() as? MessageItem.MessageData)?.message?.let {
                    getMessageDate(it.dateLong)
                } != messageDate) {
                itemList.add(MessageItem.DateSeparator(messageDate))
            }
            itemList.add(MessageItem.MessageData(message))
            messageAdapter.notifyItemInserted(itemList.size - 1)
            rvMessages.scrollToPosition(itemList.size - 1)

        } catch (e: Exception) {
            Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMessages(phoneNumber: String) {
        val targetDigits = phoneNumber.replace(Regex("[^0-9]"), "")
        val allMessages = mutableListOf<Message>()

        fun processCursor(uri: Uri, type: Int) {
            val cursor = contentResolver.query(uri, arrayOf("body", "date", "type", "address"), null, null, "date ASC")
            cursor?.use {
                while (it.moveToNext()) {
                    val rawAddress = it.getString(it.getColumnIndexOrThrow("address"))
                    val dbDigits = rawAddress.replace(Regex("[^0-9]"), "")

                    if (dbDigits == targetDigits) {
                        val body = it.getString(it.getColumnIndexOrThrow("body"))
                        val date = it.getLong(it.getColumnIndexOrThrow("date"))

                        allMessages.add(
                            Message(
                                body = body ?: "",
                                timestamp = formatTime(date),
                                isReceived = type == 1,
                                dateLong = date
                            )
                        )
                    }
                }
            }
        }

        processCursor(Uri.parse("content://sms/inbox"), 1)
        processCursor(Uri.parse("content://sms/sent"), 2)

        allMessages.sortBy { it.dateLong }
        itemList.clear()

        var lastDate = ""
        for (msg in allMessages) {
            val msgDate = getMessageDate(msg.dateLong)
            if (msgDate != lastDate) {
                itemList.add(MessageItem.DateSeparator(msgDate))
                lastDate = msgDate
            }
            itemList.add(MessageItem.MessageData(msg))
        }

        messageAdapter.notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

    private fun getMessageDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
            cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
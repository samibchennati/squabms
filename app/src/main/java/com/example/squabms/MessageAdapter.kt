package com.example.squabms

import android.content.Context
import android.graphics.Color
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private val items: List<MessageItem>, private val context: Context, private val phoneNumber: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_MESSAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MessageItem.DateSeparator -> TYPE_DATE
            is MessageItem.MessageData -> TYPE_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_seperator, parent, false)
                DateViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message, parent, false)
                MessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateViewHolder -> holder.bind(items[position] as MessageItem.DateSeparator)
            is MessageViewHolder -> holder.bind((items[position] as MessageItem.MessageData).message)
        }
    }

    override fun getItemCount() = items.size

    private fun getCarrierName(): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.networkOperatorName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getMyPhoneNumber(): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.line1Number?.takeLast(4) ?: "0000"
        } catch (e: Exception) {
            "0000"
        }
    }

    private fun formatTimestamp(dateLong: Long): String {
        val date = Date(dateLong)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val timeFormat = SimpleDateFormat("h:mma", Locale.US)
        val rawTime = timeFormat.format(date).lowercase(Locale.US)

        val formattedTime = when {
            rawTime.endsWith("am") -> rawTime.replace("am", "a")
            rawTime.endsWith("pm") -> rawTime.replace("pm", "p")
            else -> rawTime
        }

        val formattedDate = dateFormat.format(date)
        val carrierName = getCarrierName()
        val myLastFour = getMyPhoneNumber()

        return "$formattedDate, $formattedTime, $carrierName $myLastFour Text"
    }

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(dateSeparator: MessageItem.DateSeparator) {
            tvDate.text = dateSeparator.date
        }
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)

        fun bind(message: Message) {
            tvMessage.text = message.body
            tvTimestamp.text = formatTimestamp(message.dateLong)

            val params = messageContainer.layoutParams as LinearLayout.LayoutParams
            if (message.isReceived) {
                params.gravity = Gravity.START
                messageContainer.setBackgroundColor(Color.parseColor("#333333"))
            } else {
                params.gravity = Gravity.END
                messageContainer.setBackgroundColor(Color.parseColor("#007AFF"))
            }
            messageContainer.layoutParams = params
        }
    }
}
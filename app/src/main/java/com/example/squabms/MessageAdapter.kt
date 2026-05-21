package com.example.squabms

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val items: List<MessageItem>) :
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
            tvTimestamp.text = message.timestamp

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
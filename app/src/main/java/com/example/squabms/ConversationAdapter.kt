package com.example.squabms

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Conversation(
    val contactName: String,
    val lastMessage: String,
    val timestamp: String,
    val avatarResId: Int,
    val timestampLong: Long = 0,
    val phoneNumber: String,
    val contactId: String?
)

class ConversationAdapter(
    private val conversations: MutableList<Conversation>,
    private val context: Context,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.bind(conversation)
        holder.itemView.setOnClickListener {
            onItemClick(conversation.phoneNumber)
        }
    }

    override fun getItemCount() = conversations.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivContactPhoto: ImageView = itemView.findViewById(R.id.ivContactPhoto)
        private val tvContactName: TextView = itemView.findViewById(R.id.tvContactName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(conversation: Conversation) {
            tvContactName.text = conversation.contactName
            tvLastMessage.text = conversation.lastMessage
            tvTimestamp.text = conversation.timestamp
            loadContactPhoto(conversation.contactId)
        }

        private fun loadContactPhoto(contactId: String?) {
            if (contactId == null) {
                ivContactPhoto.setImageResource(R.drawable.ic_contact)
                return
            }

            try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.Contacts.CONTENT_URI,
                    contactId
                )
                val photoUri = Uri.withAppendedPath(uri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
                val inputStream = context.contentResolver.openInputStream(photoUri)

                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    ivContactPhoto.setImageBitmap(bitmap)
                    inputStream.close()
                } else {
                    ivContactPhoto.setImageResource(R.drawable.ic_contact)
                }
            } catch (e: Exception) {
                ivContactPhoto.setImageResource(R.drawable.ic_contact)
            }
        }
    }
}
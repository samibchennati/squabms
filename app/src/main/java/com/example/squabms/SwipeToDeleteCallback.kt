package com.example.squabms

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToDeleteCallback(
    private val adapter: ConversationAdapter,
    private val contentResolver: ContentResolver,
    private val conversationList: MutableList<Conversation>
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val conversation = conversationList[position]
        val phoneNumber = conversation.phoneNumber

        // 1. Delete messages from DB
        deleteMessagesForNumber(contentResolver, phoneNumber)

        // 2. Remove from UI list
        conversationList.removeAt(position)
        adapter.notifyItemRemoved(position)
    }

    private fun deleteMessagesForNumber(contentResolver: ContentResolver, phoneNumber: String) {
        if (phoneNumber.isEmpty()) return

        val targetDigits = phoneNumber.replace(Regex("[^0-9]"), "")
        val smsUri = Uri.parse("content://sms")

        var deletedCount = 0

        try {
            val cursor = contentResolver.query(
                smsUri,
                arrayOf("_id", "address"),
                null,
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                    val address = it.getString(it.getColumnIndexOrThrow("address")) ?: ""

                    if (address.replace(Regex("[^0-9]"), "") == targetDigits) {
                        val rows = contentResolver.delete(
                            smsUri,
                            "_id = ?",
                            arrayOf(id.toString())
                        )
                        deletedCount += rows
                        Log.d("DELETE_DEBUG", "Deleted $rows row(s) with ID: $id")
                    }
                }
            }

            Log.d("DELETE_DEBUG", "Total deleted: $deletedCount messages from $phoneNumber")

        } catch (e: Exception) {
            Log.e("DELETE_DEBUG", "Error deleting messages: ${e.message}", e)
        }
    }
}
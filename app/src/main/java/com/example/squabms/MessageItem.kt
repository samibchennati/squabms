package com.example.squabms

sealed class MessageItem {
    data class DateSeparator(val date: String) : MessageItem()
    data class MessageData(val message: Message) : MessageItem()
}
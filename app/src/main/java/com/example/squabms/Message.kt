package com.example.squabms

data class Message(
    val body: String,
    val timestamp: String,
    val isReceived: Boolean,
    val dateLong: Long = 0
)
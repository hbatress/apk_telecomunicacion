package com.example.myhouse

import android.content.Context

fun isValidEmail(email: String): Boolean {
    val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    return email.matches(emailPattern.toRegex())
}

fun saveUserIdToCache(context: Context, userId: String) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        remove("user_id") // Remove any existing user ID
        putString("user_id", userId) // Save the new user ID
        apply()
    }
}

fun getUserIdFromCache(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("user_id", null)
}

package com.legbit.scamhunter

import android.content.Context

object SpamDetector {
    private const val PREFS_NAME = "scamhunter_settings"
    private const val BLOCKED_NUMBERS = "blocked_numbers"

    private fun normalize(number: String): String {
        val cleaned = number.filter { it.isDigit() || it == '+' }
        return when {
            cleaned.startsWith("+62") -> "0" + cleaned.drop(3)
            cleaned.startsWith("0062") -> "0" + cleaned.drop(4)
            else -> cleaned
        }
    }

    fun getBlockedNumbers(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(BLOCKED_NUMBERS, emptySet())?.toSet() ?: emptySet()
    }

    fun addBlockedNumber(context: Context, number: String): Boolean {
        val normalized = normalize(number)
        if (normalized.length < 5) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val numbers = getBlockedNumbers(context).toMutableSet()
        numbers.add(normalized)
        prefs.edit().putStringSet(BLOCKED_NUMBERS, numbers).apply()
        return true
    }

    fun removeBlockedNumber(context: Context, number: String) {
        val normalized = normalize(number)
        val numbers = getBlockedNumbers(context).toMutableSet()
        numbers.remove(normalized)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(BLOCKED_NUMBERS, numbers)
            .apply()
    }

    fun isBlocked(context: Context, number: String): Boolean {
        return normalize(number) in getBlockedNumbers(context)
    }
}

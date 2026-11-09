package com.mthtamim.smartdialer

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only storage. Everything Smart Dialer remembers lives in one
 * SharedPreferences file on the phone. No account, no server, no sync.
 */
data class RecentEntry(
    val number: String,
    val name: String,
    val providerId: String,
    val timeMillis: Long,
    val durationSec: Long
)

object Store {
    private const val PREFS = "smart_dialer"
    private const val KEY_DEFAULT_PROVIDER = "default_provider"
    private const val KEY_AUTO_CALL = "auto_call"
    private const val KEY_SIM_SLOT = "sim_slot"
    private const val KEY_FAVOURITES = "favourites"
    private const val KEY_RECENTS = "recents"
    private const val MAX_RECENTS = 50

    fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun normalize(number: String): String {
        return number.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
    }

    fun displayNumber(number: String): String {
        return number
    }

    fun defaultProviderId(context: Context): String {
        return prefs(context).getString(KEY_DEFAULT_PROVIDER, Providers.SIM_ID)
            ?: Providers.SIM_ID
    }

    fun setDefaultProviderId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_DEFAULT_PROVIDER, id).apply()
    }

    fun autoCallOn(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_CALL, true)
    }

    fun setAutoCallOn(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_CALL, on).apply()
    }

    /** 0 = SIM 1, 1 = SIM 2. Best effort: the system decides the order
     * of its call-capable accounts, Smart Dialer only picks by index. */
    fun simSlot(context: Context): Int {
        return prefs(context).getInt(KEY_SIM_SLOT, 0)
    }

    fun setSimSlot(context: Context, slot: Int) {
        prefs(context).edit().putInt(KEY_SIM_SLOT, slot).apply()
    }

    fun favourites(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_FAVOURITES, emptySet()) ?: emptySet()
    }

    fun isFavourite(context: Context, number: String): Boolean {
        return favourites(context).contains(normalize(number))
    }

    fun setFavourite(context: Context, number: String, fav: Boolean) {
        val current = favourites(context).toMutableSet()
        val key = normalize(number)
        if (fav) {
            current.add(key)
        } else {
            current.remove(key)
        }
        prefs(context).edit().putStringSet(KEY_FAVOURITES, current).apply()
    }

    fun recents(context: Context): List<RecentEntry> {
        val raw = prefs(context).getString(KEY_RECENTS, "[]") ?: "[]"
        val out = ArrayList<RecentEntry>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                out.add(
                    RecentEntry(
                        number = obj.optString("number", ""),
                        name = obj.optString("name", ""),
                        providerId = obj.optString("provider", Providers.SIM_ID),
                        timeMillis = obj.optLong("time", 0L),
                        durationSec = obj.optLong("duration", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            // A corrupt recents list must never crash the dialer.
            // The list is dropped and starts empty again.
            out.clear()
        }
        return out
    }

    fun addRecent(context: Context, entry: RecentEntry) {
        val list = recents(context).toMutableList()
        list.add(0, entry)
        saveRecents(context, list.take(MAX_RECENTS))
    }

    fun updateLastRecentDuration(context: Context, durationSec: Long) {
        val list = recents(context).toMutableList()
        if (list.isEmpty()) {
            return
        }
        val first = list[0]
        list[0] = first.copy(durationSec = durationSec)
        saveRecents(context, list)
    }

    private fun saveRecents(context: Context, list: List<RecentEntry>) {
        val arr = JSONArray()
        for (entry in list) {
            val obj = JSONObject()
            obj.put("number", entry.number)
            obj.put("name", entry.name)
            obj.put("provider", entry.providerId)
            obj.put("time", entry.timeMillis)
            obj.put("duration", entry.durationSec)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    fun clearRecents(context: Context) {
        prefs(context).edit().putString(KEY_RECENTS, "[]").apply()
    }
}

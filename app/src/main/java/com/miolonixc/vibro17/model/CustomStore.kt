package com.miolonixc.vibro17.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists user-created effects to an internal JSON file so they survive
 * app restarts. Uses only the built-in `org.json` — no extra dependency.
 */
object CustomStore {

    private const val FILE = "custom_effects.json"
    private const val ICON = "✨"

    fun load(context: Context): List<VibroEffect> {
        val file = context.getFileStreamPath(FILE)
        if (!file.exists()) return emptyList()
        return try {
            val text = file.bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            (0 until array.length()).mapNotNull { i ->
                runCatching { fromJson(array.getJSONObject(i)) }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, effect: VibroEffect) {
        val all = load(context).toMutableList()
        all.removeAll { it.id == effect.id }
        all.add(effect)
        save(context, all)
    }

    fun save(context: Context, list: List<VibroEffect>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        context.openFileOutput(FILE, Context.MODE_PRIVATE).use {
            it.write(array.toString().toByteArray())
        }
    }

    private fun toJson(e: VibroEffect): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("title", e.title)
        put("subtitle", e.subtitle)
        put("icon", e.icon)
        put("repeat", e.repeat)
        put("timings", JSONArray(e.timings.toList()))
        put("amplitudes", JSONArray(e.amplitudes.toList()))
    }

    private fun fromJson(o: JSONObject): VibroEffect = VibroEffect(
        id = o.getString("id"),
        title = o.getString("title"),
        subtitle = o.optString("subtitle", "Свой эффект"),
        icon = o.optString("icon", ICON),
        timings = LongArray(o.getJSONArray("timings").length()) { i ->
            o.getJSONArray("timings").getLong(i)
        },
        amplitudes = IntArray(o.getJSONArray("amplitudes").length()) { i ->
            o.getJSONArray("amplitudes").getInt(i)
        },
        repeat = o.optInt("repeat", 0)
    )

    fun newId(): String = "custom_${System.currentTimeMillis()}"
}

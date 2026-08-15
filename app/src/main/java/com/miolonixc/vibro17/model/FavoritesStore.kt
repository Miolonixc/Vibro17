package com.miolonixc.vibro17.model

import android.content.Context
import android.content.SharedPreferences

/** Persists the set of favorited effect ids. */
object FavoritesStore {

    private const val PREFS = "vibro_favorites"
    private const val KEY = "ids"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isFavorite(context: Context, id: String): Boolean =
        prefs(context).getStringSet(KEY, emptySet())?.contains(id) == true

    fun toggle(context: Context, id: String) {
        val set = prefs(context).getStringSet(KEY, mutableSetOf())!!.toMutableSet()
        if (!set.add(id)) set.remove(id)
        prefs(context).edit().putStringSet(KEY, set).apply()
    }

    fun all(context: Context): Set<String> =
        prefs(context).getStringSet(KEY, emptySet()) ?: emptySet()
}

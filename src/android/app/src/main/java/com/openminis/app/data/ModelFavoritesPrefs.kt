package com.openminis.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * [T-android-model-favorites] The user's favorited model entries, persisted
 * per device as a set of config entry ids (config.modelEntries.id — stable
 * across restarts; a deleted model's stale id simply never matches).
 *
 * Favorites surface in the chat model picker: a pinned "收藏" card at the top
 * of the list, a star toggle on every model row, and priority ordering
 * (active model → favorites → checked) inside each provider section.
 */
object ModelFavoritesPrefs {
    private const val PREFS = "minis_model_favorites_prefs"
    private const val KEY_FAVORITES = "favoriteModelEntryIds"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet()

    fun save(context: Context, favorites: Set<String>) {
        prefs(context).edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }
}

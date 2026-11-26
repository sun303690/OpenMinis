package com.openminis.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * [T-android-model-auto-check] The user's "automatically check available
 * models and keep only those checked" choice, persisted per device.
 *
 * When enabled, every time a provider's model list is fetched or refreshed
 * (after saving an API key, a manual Refresh, an OAuth sign-in) the app
 * probes each visible model with the SAME real-provider smoke test the
 * manual "自检已选模型" sheet uses, then auto-applies the result:
 * available → checked, unavailable → unchecked.
 *
 * Defaults to OFF: the probe is a real request per model, and silently
 * billing dozens of models on a paid provider just because the user refreshed
 * a list is not a default we should choose for them. The toggle lives on each
 * provider detail screen (Models section) and applies app-wide.
 */
object ModelAutoCheckPrefs {
    private const val PREFS = "minis_model_auto_check_prefs"
    private const val KEY_ENABLED = "modelAutoCheckEnabled"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}

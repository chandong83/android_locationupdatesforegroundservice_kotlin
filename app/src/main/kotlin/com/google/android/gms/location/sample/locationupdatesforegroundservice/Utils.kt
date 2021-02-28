package com.google.android.gms.location.sample.locationupdatesforegroundservice

import android.content.Context
import android.location.Location
import java.text.DateFormat
import androidx.preference.PreferenceManager
import java.util.*

class Utils {
    companion object {
        const val KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates"

        fun requestingLocationUpdates(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false)
        }

        fun setRequestingLocationUpdates(context: Context, requestingLocationUpdates: Boolean) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                    .apply()
        }
        fun getLocationText(location: Location?): String {
            return if(location == null) {
                "Unknown location"
            } else {
                "(${location.latitude}, ${location.longitude})"
            }
        }

        fun getLocationTitle(context: Context): String {
            return context.getString(R.string.location_updated,
                    DateFormat.getDateTimeInstance().format(Date()))
        }
    }
}
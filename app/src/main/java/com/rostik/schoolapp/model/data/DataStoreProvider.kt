package com.rostik.schoolapp.model.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

object DataStoreProvider {
    private const val DATASTORE_NAME = "school_app"

    val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(DATASTORE_NAME)
}
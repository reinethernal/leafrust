package com.leafrust.data.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.modelDataStore by preferencesDataStore(name = "leafrust_model_prefs")

class ModelPreferences(private val context: Context) {
    private val keyId = stringPreferencesKey("selected_model_id")

    val selectedModelId: Flow<String> = context.modelDataStore.data.map { prefs ->
        prefs[keyId] ?: ModelCatalog.defaultId(context)
    }

    suspend fun getSelectedModelId(): String = selectedModelId.first()

    suspend fun setSelectedModelId(id: String) {
        context.modelDataStore.edit { it[keyId] = id }
    }

    suspend fun selectedSpec(): ModelSpec = ModelCatalog.byId(context, getSelectedModelId())
}

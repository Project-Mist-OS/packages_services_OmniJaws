/*
 * Copyright 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnirom.omnijaws.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import com.android.internal.util.mist.OmniJawsClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.omnirom.omnijaws.Config
import org.omnirom.omnijaws.WeatherUpdateService
import org.omnirom.omnijaws.widget.WeatherAppWidgetProvider
import org.omnirom.omnijaws.WeatherIconPackManager

data class IconPackItem(
    val label: String,
    val value: String,
    val supportsVariants: Boolean
)

data class SettingsUiState(
    val enabled: Boolean = false,
    val provider: String = "1",
    val units: String = "0",
    val updateInterval: String = "2",
    val customLocation: Boolean = false,
    val locationName: String = "",
    val iconPack: String = "",
    val iconVariantMode: String = Config.ICON_VARIANT_AUTO,
    val owmKey: String = "",
    val lastUpdateTime: String = "",
    val iconPacks: List<IconPackItem> = emptyList(),
    val hasLocationPermission: Boolean = false
) {
    val providerLabel: String get() = when (provider) {
        "0" -> "OpenWeatherMap"
        "1" -> "MET Norway"
        else -> provider
    }
    val unitsLabel: String get() = when (units) {
        "0" -> "Metric (\u00b0C)"
        "1" -> "Imperial (\u00b0F)"
        else -> units
    }
    val intervalLabel: String get() = when (updateInterval) {
        "1" -> "1 hour"
        "2" -> "2 hours"
        "4" -> "4 hours"
        "6" -> "6 hours"
        "12" -> "12 hours"
        else -> "$updateInterval hours"
    }
    val selectedIconPack: IconPackItem?
        get() = iconPacks.firstOrNull { it.value == iconPack }

    val iconPackLabel: String
        get() = selectedIconPack?.label ?: iconPack

    val selectedIconPackSupportsVariants: Boolean
        get() = selectedIconPack?.supportsVariants == true

    val iconVariantLabel: String get() = when (iconVariantMode) {
        Config.ICON_VARIANT_LIGHT -> "Light"
        Config.ICON_VARIANT_DARK -> "Dark"
        else -> "Automatic"
    }

    val iconVariantOptions: List<Pair<String, String>> get() = listOf(
        Config.ICON_VARIANT_AUTO to "Automatic",
        Config.ICON_VARIANT_LIGHT to "Light",
        Config.ICON_VARIANT_DARK to "Dark"
    )

}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val ctx get() = getApplication<Application>()

    fun loadSettings() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
        val iconPacks = loadIconPacks()

        _uiState.value = SettingsUiState(
            enabled = Config.isEnabled(ctx),
            provider = prefs.getString(Config.PREF_KEY_PROVIDER, "1") ?: "1",
            units = prefs.getString(Config.PREF_KEY_UNITS, "0") ?: "0",
            updateInterval = prefs.getString(Config.PREF_KEY_UPDATE_INTERVAL, "2") ?: "2",
            customLocation = prefs.getBoolean(Config.PREF_KEY_CUSTOM_LOCATION, false),
            locationName = Config.getLocationName(ctx) ?: "",
            iconPack = Config.getIconPack(ctx) ?: DEFAULT_ICON_PACK,
            iconVariantMode = Config.getIconVariantMode(ctx),
            iconPacks = iconPacks,
            owmKey = Config.getOwmKey(ctx) ?: "",
            lastUpdateTime = queryLastUpdate(),
            hasLocationPermission = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun setEnabled(enabled: Boolean) {
        Config.setEnabled(ctx, enabled)
        _uiState.value = _uiState.value.copy(enabled = enabled)
        if (enabled) {
            WeatherUpdateService.scheduleUpdatePeriodic(ctx)
        } else {
            WeatherUpdateService.cancelAllUpdate(ctx)
            WeatherAppWidgetProvider.disableAllWidgets(ctx)
            WeatherUpdateService.disabledCall(ctx)
        }
    }

    fun setProvider(value: String) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
        prefs.edit().putString(Config.PREF_KEY_PROVIDER, value).commit()
        _uiState.value = _uiState.value.copy(provider = value)
        scheduleUpdate()
    }

    fun setUnits(value: String) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
        prefs.edit().putString(Config.PREF_KEY_UNITS, value).commit()
        _uiState.value = _uiState.value.copy(units = value)
        scheduleUpdate()
    }

    fun setUpdateInterval(value: String) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
        prefs.edit().putString(Config.PREF_KEY_UPDATE_INTERVAL, value).commit()
        _uiState.value = _uiState.value.copy(updateInterval = value)
        scheduleUpdate()
    }

    fun setCustomLocation(enabled: Boolean) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
        prefs.edit().putBoolean(Config.PREF_KEY_CUSTOM_LOCATION, enabled).commit()
        _uiState.value = _uiState.value.copy(customLocation = enabled)
        scheduleUpdate()
    }

    fun setLocationResult(name: String, lat: Double, lon: Double) {
        val locationId = String.format(java.util.Locale.US, "lat=%f&lon=%f", lat, lon)
        Config.setLocationId(ctx, locationId)
        Config.setLocationName(ctx, name)
        _uiState.value = _uiState.value.copy(locationName = name)
        scheduleUpdate()
    }

    fun setIconPack(value: String) {
        val selectedPack = _uiState.value.iconPacks.firstOrNull { it.value == value }
        val variantMode = if (selectedPack?.supportsVariants == true) {
            _uiState.value.iconVariantMode
        } else {
            Config.ICON_VARIANT_AUTO
        }

        Config.setIconPack(ctx, value)
        Config.setIconVariantMode(ctx, variantMode)
        _uiState.value = _uiState.value.copy(
            iconPack = value,
            iconVariantMode = variantMode
        )
        scheduleUpdate()
    }

    fun setIconVariantMode(value: String) {
        Config.setIconVariantMode(ctx, value)
        _uiState.value = _uiState.value.copy(iconVariantMode = value)
        scheduleUpdate()
    }

    fun setOwmKey(value: String) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
        prefs.edit().putString(Config.PREF_KEY_OWM_KEY, value).commit()
        _uiState.value = _uiState.value.copy(owmKey = value)
        scheduleUpdate()
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasLocationPermission = granted)
        if (granted) scheduleUpdate()
    }

    fun refreshUpdateStatus() {
        _uiState.value = _uiState.value.copy(lastUpdateTime = queryLastUpdate())
    }

    private fun scheduleUpdate() {
        WeatherUpdateService.scheduleUpdateNow(ctx)
    }

    private fun queryLastUpdate(): String {
        OmniJawsClient.get().queryWeather(ctx)
        return OmniJawsClient.get().getWeatherInfo()?.getLastUpdateTime() ?: ""
    }

    private fun loadIconPacks(): List<IconPackItem> {
        return WeatherIconPackManager.load(ctx).map {
            IconPackItem(
                label = it.label,
                value = it.value,
                supportsVariants = it.hasExplicitVariants()
            )
        }
    }

    companion object {
        const val DEFAULT_ICON_PACK = "org.omnirom.omnijaws.google_new"
    }
}

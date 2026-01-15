package com.green.hwinfo.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class TileShape {
    SQUARE, CIRCLE, TRIANGLE
}

data class TileConfig(
    val originalLabel: String,
    var customLabel: String? = null,
    var customColor: Long? = null,
    var customTitleColor: Long? = null,
    var customValueColor: Long? = null,
    var titleSizeScale: Float = 1.0f,
    var valueSizeScale: Float = 1.0f,
    var isHidden: Boolean = false,
    var gridX: Int = 0,
    var gridY: Int = 0,
    var spanX: Int = 1,
    var spanY: Int = 1,
    var shape: TileShape? = TileShape.SQUARE
)

data class AppConfig(
    var backgroundImageUri: String? = null,
    var useGlassEffect: Boolean = true,
    var gridSizeDp: Int = 120
)

class SensorConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sensor_config", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTileConfigs(configs: List<TileConfig>) {
        val json = gson.toJson(configs)
        prefs.edit().putString("tile_configs", json).apply()
    }

    fun getTileConfigs(): MutableList<TileConfig> {
        val json = prefs.getString("tile_configs", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<TileConfig>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAppConfig(config: AppConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString("app_config", json).apply()
    }

    fun getAppConfig(): AppConfig {
        val json = prefs.getString("app_config", null) ?: return AppConfig()
        return gson.fromJson(json, AppConfig::class.java)
    }
}

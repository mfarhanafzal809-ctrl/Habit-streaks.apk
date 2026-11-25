package com.example.data.repository

import com.example.data.local.AppSettingDao
import com.example.data.model.AppSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val appSettingDao: AppSettingDao
) {
    val isDarkMode: Flow<Boolean?> = appSettingDao.getSetting("dark_mode").map { it?.toBoolean() }
    val isVibrationEnabled: Flow<Boolean> = appSettingDao.getSetting("global_vibration").map { it?.toBoolean() ?: true }
    val isNotificationsEnabled: Flow<Boolean> = appSettingDao.getSetting("global_notifications").map { it?.toBoolean() ?: true }

    val isDynamicColorEnabled: Flow<Boolean> = appSettingDao.getSetting("dynamic_color").map { it?.toBoolean() ?: false }
    val isDynamicRainbowEnabled: Flow<Boolean> = appSettingDao.getSetting("dynamic_rainbow_enabled").map { it?.toBoolean() ?: false }
    val themeTemplate: Flow<String> = appSettingDao.getSetting("theme_template").map { it ?: "INDIGO" }
    val savedCustomThemes: Flow<String> = appSettingDao.getSetting("saved_custom_themes").map { it ?: "" }
    val dynamicColorSource: Flow<String> = appSettingDao.getSetting("dynamic_color_source").map { it ?: "BOTH" }
    val hasCompletedOnboarding: Flow<Boolean> = appSettingDao.getSetting("onboarding_complete").map { it?.toBoolean() ?: false }
    val isGlassmorphismEnabled: Flow<Boolean> = appSettingDao.getSetting("glassmorphism_enabled").map { it?.toBoolean() ?: false }
    val glassTint: Flow<String> = appSettingDao.getSetting("glass_vfx_tint").map { it ?: "THEME" }
    val glassCurvature: Flow<Float> = appSettingDao.getSetting("glass_vfx_curvature").map { it?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.50f }
    val glassBloom: Flow<Float> = appSettingDao.getSetting("glass_vfx_bloom").map { it?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.70f }
    val glassReflection: Flow<Float> = appSettingDao.getSetting("glass_vfx_reflection").map { it?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.65f }
    val glassTranslucency: Flow<Float> = appSettingDao.getSetting("glass_vfx_translucency").map { it?.toFloatOrNull()?.coerceIn(0.15f, 1f) ?: 0.68f }
    val glassAuroraEnabled: Flow<Boolean> = appSettingDao.getSetting("glass_vfx_aurora").map { it?.toBoolean() ?: true }
    val glassSpecularGloss: Flow<Boolean> = appSettingDao.getSetting("glass_vfx_specular_gloss").map { it?.toBoolean() ?: true }
    val glassCausticGlow: Flow<Boolean> = appSettingDao.getSetting("glass_vfx_caustic_glow").map { it?.toBoolean() ?: true }
    val adFreeUntilTimestamp: Flow<Long> = appSettingDao.getSetting("ad_free_until_timestamp").map { it?.toLongOrNull() ?: 0L }
    val is24HourFormat: Flow<Boolean> = appSettingDao.getSetting("is_24_hour_format").map { it?.toBoolean() ?: false }
    val userName: Flow<String> = appSettingDao.getSetting("user_name").map { it ?: "Champion" }

    // Widget specific settings
    val isWidgetEnabled: Flow<Boolean> = appSettingDao.getSetting("widget_enabled").map { it?.toBoolean() ?: true }
    val isWidgetUrgentHighlightEnabled: Flow<Boolean> = appSettingDao.getSetting("widget_urgent_highlight").map { it?.toBoolean() ?: true }
    val isWidgetShowCompleted: Flow<Boolean> = appSettingDao.getSetting("widget_show_completed").map { it?.toBoolean() ?: true }
    val isWidgetQuickControlsEnabled: Flow<Boolean> = appSettingDao.getSetting("widget_quick_controls").map { it?.toBoolean() ?: true }
    val widgetBgOpacity: Flow<Float> = appSettingDao.getSetting("widget_bg_opacity").map { it?.toFloatOrNull() ?: 0.8f }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        appSettingDao.setSetting(AppSetting("onboarding_complete", completed.toString()))
    }
    
    suspend fun setWidgetBgOpacity(opacity: Float) {
        appSettingDao.setSetting(AppSetting("widget_bg_opacity", opacity.toString()))
    }

    suspend fun setGlassmorphismEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("glassmorphism_enabled", enabled.toString()))
    }

    suspend fun saveGlassVfxConfig(config: com.example.ui.theme.GlassVfxConfig) {
        val clean = config.clamped()
        appSettingDao.setSetting(AppSetting("glassmorphism_enabled", clean.enabled.toString()))
        appSettingDao.setSetting(AppSetting("glass_vfx_tint", clean.tint))
        appSettingDao.setSetting(AppSetting("glass_vfx_curvature", clean.lensCurvature.toString()))
        appSettingDao.setSetting(AppSetting("glass_vfx_bloom", clean.highlightBloom.toString()))
        appSettingDao.setSetting(AppSetting("glass_vfx_reflection", clean.reflectionDepth.toString()))
        appSettingDao.setSetting(AppSetting("glass_vfx_translucency", clean.frostedTranslucency.toString()))
        appSettingDao.setSetting(AppSetting("glass_vfx_aurora", clean.auroraBackdrop.toString()))
        appSettingDao.setSetting(AppSetting("glass_vfx_specular_gloss", clean.specularGloss.toString()))
        appSettingDao.setSetting(AppSetting("glass_vfx_caustic_glow", clean.causticGlow.toString()))
    }

    suspend fun setWidgetEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("widget_enabled", enabled.toString()))
    }

    suspend fun setWidgetUrgentHighlightEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("widget_urgent_highlight", enabled.toString()))
    }

    suspend fun setWidgetShowCompleted(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("widget_show_completed", enabled.toString()))
    }

    suspend fun setWidgetQuickControlsEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("widget_quick_controls", enabled.toString()))
    }

    suspend fun setDarkMode(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("dark_mode", enabled.toString()))
    }

    suspend fun setThemeTemplate(template: String) {
        appSettingDao.setSetting(AppSetting("theme_template", template))
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("dynamic_color", enabled.toString()))
    }

    suspend fun setDynamicRainbowEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("dynamic_rainbow_enabled", enabled.toString()))
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("global_vibration", enabled.toString()))
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appSettingDao.setSetting(AppSetting("global_notifications", enabled.toString()))
    }

    suspend fun setAdFreeUntil(timestampMillis: Long) {
        appSettingDao.setSetting(AppSetting("ad_free_until_timestamp", timestampMillis.toString()))
    }

    suspend fun set24HourFormat(is24H: Boolean) {
        appSettingDao.setSetting(AppSetting("is_24_hour_format", is24H.toString()))
    }

    suspend fun setUserName(name: String) {
        appSettingDao.setSetting(AppSetting("user_name", name))
    }

    suspend fun setSavedCustomThemes(themes: String) {
        appSettingDao.setSetting(AppSetting("saved_custom_themes", themes))
    }

    suspend fun setDynamicColorSource(source: String) {
        appSettingDao.setSetting(AppSetting("dynamic_color_source", source))
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return appSettingDao.getSettingValue(key) ?: defaultValue
    }

    suspend fun setSetting(key: String, value: String) {
        appSettingDao.setSetting(AppSetting(key, value))
    }
}


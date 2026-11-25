package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HabitDatabase
import com.example.data.repository.SettingsRepository
import com.example.ui.theme.GlassTintPreset
import com.example.ui.theme.GlassVfxConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GlassVfxConfigAndRepoTest {

    private lateinit var context: Context
    private lateinit var db: HabitDatabase
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsRepo = SettingsRepository(db.appSettingDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun glassTintPresetResolutionAndDefaultsWorkAccurately() {
        assertEquals(GlassTintPreset.CYAN, GlassTintPreset.fromKey("cyan"))
        assertEquals(GlassTintPreset.INDIGO, GlassTintPreset.fromKey("INDIGO"))
        assertEquals(GlassTintPreset.GOLD, GlassTintPreset.fromKey("gold"))
        assertEquals(GlassTintPreset.ROSE, GlassTintPreset.fromKey("Rose"))
        assertEquals(GlassTintPreset.THEME, GlassTintPreset.fromKey("unknown_tint"))
    }

    @Test
    fun glassVfxConfigClampsValuesAndFormatsSummaryCorrectly() {
        val raw = GlassVfxConfig(
            enabled = true,
            tint = "GOLD",
            lensCurvature = 1.8f,
            highlightBloom = -0.3f,
            reflectionDepth = 0.65f,
            frostedTranslucency = 0.02f,
            auroraBackdrop = false,
            specularGloss = true,
            causticGlow = false
        ).clamped()

        assertEquals(1.0f, raw.lensCurvature, 0.001f)
        assertEquals(0.0f, raw.highlightBloom, 0.001f)
        assertEquals(0.65f, raw.reflectionDepth, 0.001f)
        assertEquals(0.15f, raw.frostedTranslucency, 0.001f)
        assertEquals("Curvature: 100% • Bloom: 0% • Reflection: 65%", raw.summaryText())

        val (glow1, glow2) = raw.resolveGlowColors(
            themePrimary = Color(0xFF4F46E5),
            themeSecondary = Color(0xFF06B6D4),
            isDark = true
        )
        assertEquals(GlassTintPreset.GOLD.darkPrimaryGlow, glow1)
        assertEquals(GlassTintPreset.GOLD.darkSecondaryGlow, glow2)
    }

    @Test
    fun settingsRepositoryPersistsAndLoadsAllGlassVfxConfigFields() = runBlocking {
        val customConfig = GlassVfxConfig(
            enabled = true,
            tint = "ROSE",
            lensCurvature = 0.82f,
            highlightBloom = 0.42f,
            reflectionDepth = 0.91f,
            frostedTranslucency = 0.55f,
            auroraBackdrop = false,
            specularGloss = false,
            causticGlow = true
        )

        settingsRepo.saveGlassVfxConfig(customConfig)

        assertTrue(settingsRepo.isGlassmorphismEnabled.first())
        assertEquals("ROSE", settingsRepo.glassTint.first())
        assertEquals(0.82f, settingsRepo.glassCurvature.first(), 0.001f)
        assertEquals(0.42f, settingsRepo.glassBloom.first(), 0.001f)
        assertEquals(0.91f, settingsRepo.glassReflection.first(), 0.001f)
        assertEquals(0.55f, settingsRepo.glassTranslucency.first(), 0.001f)
        assertFalse(settingsRepo.glassAuroraEnabled.first())
        assertFalse(settingsRepo.glassSpecularGloss.first())
        assertTrue(settingsRepo.glassCausticGlow.first())
    }
}

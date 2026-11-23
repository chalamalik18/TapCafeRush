package xyz.tapcaferush.jglsrc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import xyz.tapcaferush.jglsrc.data.PreferencesRepository
import xyz.tapcaferush.jglsrc.entry.LaunchPreferences

@RunWith(AndroidJUnit4::class)
class GameEntryTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    @Test fun gameProgressSurvivesChangingTheEntryDecisionAndReopeningGame() {
        val context = screen.activity.applicationContext
        val game = PreferencesRepository(context)
        val entry = LaunchPreferences(context)
        val progress = runBlocking {
            game.recordLevelSession(1, 40, 2, 2, 10, 2, 0, 0)
            game.allLevelsStats.first().also {
                entry.save("browser", "https://example.test")
                entry.save("", "")
            }
        }
        screen.activityRule.scenario.recreate()
        screen.onNodeWithText("Choose level & play").assertIsDisplayed()
        assertEquals(progress, runBlocking { PreferencesRepository(context).allLevelsStats.first() })
    }
}

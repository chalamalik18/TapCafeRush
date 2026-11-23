package xyz.tapcaferush.jglsrc.entry

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import xyz.tapcaferush.jglsrc.MainActivity
import xyz.tapcaferush.jglsrc.ui.theme.TapCafeRushTheme

class LaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = LaunchPreferences(applicationContext)
        setContent {
            TapCafeRushTheme(darkTheme = true) {
                LaunchScreen(preferences, onOpenShell = ::openBrowser, onOpenApp = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                })
            }
        }
    }
}

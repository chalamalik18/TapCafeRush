package xyz.tapcaferush.jglsrc

import android.app.Application
import xyz.tapcaferush.jglsrc.data.PreferencesRepository

class TapCafeApp : Application() {
    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
    }
}

package de.dgstudios.spinkingdom

import android.app.Application
import de.dgstudios.spinkingdom.notifications.Notifications

class SpinKingdomApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannel(this)
    }
}

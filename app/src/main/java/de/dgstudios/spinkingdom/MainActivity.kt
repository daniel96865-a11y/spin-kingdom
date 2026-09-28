package de.dgstudios.spinkingdom

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.dgstudios.spinkingdom.navigation.SpinKingdomRoot
import de.dgstudios.spinkingdom.notifications.Notifications
import de.dgstudios.spinkingdom.ui.GameViewModel
import de.dgstudios.spinkingdom.ui.theme.SpinKingdomTheme

class MainActivity : AppCompatActivity() {
    private val vm: GameViewModel by viewModels { GameViewModel.Factory((application as SpinKingdomApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // German is the default UI language unless the player picked another one in the settings.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("de"))
        }
        enableEdgeToEdge()
        setContent {
            SpinKingdomTheme { SpinKingdomRoot(vm) }
        }
    }

    override fun onStart() {
        super.onStart()
        Notifications.cancel(this)
        vm.onForeground()
    }

    override fun onStop() {
        super.onStop()
        val full = vm.onBackground()
        if (full != null && !isChangingConfigurations) Notifications.scheduleSpinsFull(this, full)
    }
}

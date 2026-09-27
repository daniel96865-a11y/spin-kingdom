package de.danielgrebe.spinkingdom.data

import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.domain.AdOverlayState
import de.danielgrebe.spinkingdom.domain.AdResult
import de.danielgrebe.spinkingdom.domain.AdsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simulated rewarded video: shows a countdown overlay; reward is granted only if the player
 * waits until the end. Replace with AdMob RewardedAd later.
 */
class SimulatedAdsRepository(private val seconds: Int = GameBalanceConfig.AD_DURATION_SECONDS) : AdsRepository {
    private val _overlay = MutableStateFlow<AdOverlayState?>(null)
    override val overlay: StateFlow<AdOverlayState?> = _overlay.asStateFlow()
    private var closeSignal: CompletableDeferred<Boolean>? = null

    override fun isRewardedAvailable() = _overlay.value == null

    override suspend fun showRewarded(): AdResult {
        if (_overlay.value != null) return AdResult.NotAvailable
        val signal = CompletableDeferred<Boolean>()
        closeSignal = signal
        for (left in seconds downTo 1) {
            if (signal.isCompleted) break
            _overlay.value = AdOverlayState(left, seconds, canClose = false)
            delay(1000)
        }
        val finished = !signal.isCompleted
        if (finished) {
            _overlay.value = AdOverlayState(0, seconds, canClose = true)
            signal.await()
        }
        _overlay.value = null
        closeSignal = null
        return if (finished) AdResult.Rewarded else AdResult.Cancelled
    }

    /** Close button: before the end it cancels (no reward), after the end it collects the reward. */
    override fun closeOverlay() {
        closeSignal?.complete(true)
    }
}

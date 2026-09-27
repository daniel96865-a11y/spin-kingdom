package de.danielgrebe.spinkingdom.domain

import de.danielgrebe.spinkingdom.models.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Persistence of the save game. Local DataStore today, cloud save later. */
interface GameStateRepository {
    val state: Flow<GameState>
    suspend fun load(): GameState
    suspend fun save(state: GameState)
    suspend fun reset()
}

data class Opponent(
    val id: String,
    val name: String,
    val avatar: Int,
    val level: Int,
    val stages: List<Int>,
    val hasShield: Boolean,
    val stars: Int
)

/** Source of attack / raid targets. Local bots today, real players via server later. */
interface OpponentRepository {
    suspend fun findAttackTarget(playerLevel: Int): Opponent
    suspend fun findRaidTarget(playerLevel: Int): Opponent
    fun botNames(): List<String>
}

data class LeaderboardEntry(val rank: Int, val name: String, val level: Int, val stars: Int, val avatar: Int, val isPlayer: Boolean)

interface LeaderboardRepository {
    suspend fun leaderboard(player: GameState, now: Long): List<LeaderboardEntry>
}

enum class ProductKind { SPINS, COINS, CHEST, OFFER }

data class ShopProduct(
    val id: String,
    val kind: ProductKind,
    val priceLabel: String,
    val spins: Int = 0,
    val coinsU: Double = 0.0,
    val chest: de.danielgrebe.spinkingdom.models.ChestType? = null,
    val highlight: Boolean = false
)

sealed interface PurchaseResult {
    data class Success(val product: ShopProduct, val orderId: String) : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failed(val reason: String) : PurchaseResult
}

/** Shop billing. FakeBillingRepository simulates test purchases; replace with Google Play Billing later. */
interface BillingRepository {
    val isTestMode: Boolean
    fun products(): List<ShopProduct>
    suspend fun purchase(productId: String): PurchaseResult
}

data class AdOverlayState(val secondsLeft: Int, val totalSeconds: Int, val canClose: Boolean)

sealed interface AdResult {
    data object Rewarded : AdResult
    data object Cancelled : AdResult
    data object NotAvailable : AdResult
}

/** Rewarded ads. SimulatedAdsRepository shows a countdown overlay; replace with AdMob later. */
interface AdsRepository {
    val overlay: StateFlow<AdOverlayState?>
    fun isRewardedAvailable(): Boolean
    suspend fun showRewarded(): AdResult
    fun closeOverlay()
}

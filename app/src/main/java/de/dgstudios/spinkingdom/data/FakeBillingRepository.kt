package de.dgstudios.spinkingdom.data

import de.dgstudios.spinkingdom.domain.BillingRepository
import de.dgstudios.spinkingdom.domain.ProductKind
import de.dgstudios.spinkingdom.domain.PurchaseResult
import de.dgstudios.spinkingdom.domain.ShopProduct
import de.dgstudios.spinkingdom.models.ChestType
import kotlinx.coroutines.delay
import java.util.UUID

/** Test purchases only - no real money is ever charged. Replace with Google Play Billing later. */
class FakeBillingRepository : BillingRepository {
    override val isTestMode = true

    private val catalog = listOf(
        ShopProduct("offer_starter", ProductKind.OFFER, "0,99 €", spins = 100, coinsU = 40.0, chest = ChestType.GOLD, highlight = true),
        ShopProduct("offer_king", ProductKind.OFFER, "9,99 €", spins = 800, coinsU = 300.0, chest = ChestType.LEGENDARY, highlight = true),
        ShopProduct("spins_small", ProductKind.SPINS, "1,99 €", spins = 150),
        ShopProduct("spins_medium", ProductKind.SPINS, "4,99 €", spins = 450),
        ShopProduct("spins_large", ProductKind.SPINS, "9,99 €", spins = 1000),
        ShopProduct("coins_small", ProductKind.COINS, "1,99 €", coinsU = 60.0),
        ShopProduct("coins_large", ProductKind.COINS, "4,99 €", coinsU = 200.0),
        ShopProduct("chest_gold", ProductKind.CHEST, "2,99 €", chest = ChestType.GOLD),
        ShopProduct("chest_royal", ProductKind.CHEST, "5,99 €", chest = ChestType.ROYAL),
        ShopProduct("chest_legendary", ProductKind.CHEST, "12,99 €", chest = ChestType.LEGENDARY)
    )

    override fun products(): List<ShopProduct> = catalog

    override suspend fun purchase(productId: String): PurchaseResult {
        val p = catalog.firstOrNull { it.id == productId } ?: return PurchaseResult.Failed("unknown product")
        delay(700) // simulate billing flow
        return PurchaseResult.Success(p, "TEST-" + UUID.randomUUID().toString().take(8))
    }
}

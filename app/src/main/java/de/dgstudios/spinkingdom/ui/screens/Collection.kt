package de.dgstudios.spinkingdom.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dgstudios.spinkingdom.R
import de.dgstudios.spinkingdom.config.GameBalanceConfig
import de.dgstudios.spinkingdom.domain.Cards
import de.dgstudios.spinkingdom.models.GameState
import de.dgstudios.spinkingdom.models.PetType
import de.dgstudios.spinkingdom.models.Rarity
import de.dgstudios.spinkingdom.ui.components.Badge
import de.dgstudios.spinkingdom.ui.components.GameButton
import de.dgstudios.spinkingdom.ui.components.GameDialog
import de.dgstudios.spinkingdom.ui.components.GameText
import de.dgstudios.spinkingdom.ui.components.Panel
import de.dgstudios.spinkingdom.ui.components.ProgressBar
import de.dgstudios.spinkingdom.ui.components.RewardRow
import de.dgstudios.spinkingdom.ui.components.ScreenScaffold
import de.dgstudios.spinkingdom.ui.components.formatNumber
import de.dgstudios.spinkingdom.ui.draw.CardView
import de.dgstudios.spinkingdom.ui.draw.ChestIcon
import de.dgstudios.spinkingdom.ui.draw.CoinIcon
import de.dgstudios.spinkingdom.ui.draw.EnergyIcon
import de.dgstudios.spinkingdom.ui.draw.SET_NAMES
import de.dgstudios.spinkingdom.ui.draw.StarIcon
import de.dgstudios.spinkingdom.ui.draw.TreatIcon
import de.dgstudios.spinkingdom.ui.draw.chestColors
import de.dgstudios.spinkingdom.ui.draw.drawPet
import de.dgstudios.spinkingdom.ui.draw.rarityColor
import de.dgstudios.spinkingdom.ui.theme.SK
import kotlin.math.roundToInt

@Composable
fun rarityName(r: Rarity) = stringResource(when (r) { Rarity.COMMON -> R.string.rarity_common; Rarity.RARE -> R.string.rarity_rare; Rarity.EPIC -> R.string.rarity_epic; Rarity.LEGENDARY -> R.string.rarity_legendary })

@Composable
fun LockedScreen(title: String, unlockLevel: Int, onBack: () -> Unit, t: Float, art: @Composable () -> Unit) {
    ScreenScaffold(title, onBack) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            art()
            Spacer(Modifier.height(16.dp))
            GameText(stringResource(R.string.unlocks_at_level, unlockLevel), size = 20.sp, align = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            GameButton(stringResource(R.string.back_to_village), onBack)
        }
    }
}

// =================================================================== PETS
@Composable
fun PetsScreen(state: GameState, t: Float, onFeed: (PetType) -> Unit, onActivate: (PetType) -> Unit, onBack: () -> Unit) {
    val first = GameBalanceConfig.PET_UNLOCK_LEVEL.values.min()
    if (state.pets.isEmpty()) {
        LockedScreen(stringResource(R.string.menu_pets), first, onBack, t) { Canvas(Modifier.size(180.dp)) { drawPet(PetType.FOX, center, size.minDimension * 0.35f, t, locked = true) } }
        return
    }
    var selected by rememberSaveable { mutableStateOf(state.activePet ?: state.pets.first().type) }
    ScreenScaffold(stringResource(R.string.menu_pets), onBack, trailing = { Row(verticalAlignment = Alignment.CenterVertically) { TreatIcon(26.dp); GameText("${state.petTreats}", size = 16.sp) } }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PetType.entries.forEach { type ->
                    val owned = state.pets.any { it.type == type }
                    val sel = type == selected
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (sel) SK.PanelLight else Color(0x33000000)).border(2.dp, if (sel) SK.Gold else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { selected = type }.padding(6.dp).testTag("pet_${type.name}")) {
                        Canvas(Modifier.size(64.dp)) { drawPet(type, center, size.minDimension * 0.33f, t, locked = !owned) }
                        GameText(if (owned) petName(type) else stringResource(R.string.level_n, GameBalanceConfig.PET_UNLOCK_LEVEL.getValue(type)), size = 12.sp)
                        if (state.activePet == type) Badge(stringResource(R.string.active), color = SK.Green)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val pet = state.pets.firstOrNull { it.type == selected }
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(200.dp)) { drawPet(selected, center, size.minDimension * 0.33f, t, locked = pet == null) }
                    GameText(petName(selected), size = 26.sp, color = SK.GoldLight)
                    GameText(petDescription(selected), size = 14.sp, align = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    if (pet == null) {
                        GameText(stringResource(R.string.unlocks_at_level, GameBalanceConfig.PET_UNLOCK_LEVEL.getValue(selected)), size = 16.sp)
                    } else {
                        GameText(stringResource(R.string.level_n, pet.level) + if (pet.level >= GameBalanceConfig.PET_MAX_LEVEL) " (" + stringResource(R.string.max) + ")" else "", size = 18.sp)
                        val need = GameBalanceConfig.petXpForNext(pet.level)
                        ProgressBar(if (pet.level >= GameBalanceConfig.PET_MAX_LEVEL) 1f else pet.xp / need.toFloat(), Modifier.fillMaxWidth().padding(vertical = 6.dp), color = SK.Purple, height = 18.dp,
                            label = if (pet.level >= GameBalanceConfig.PET_MAX_LEVEL) stringResource(R.string.max) else "${pet.xp} / $need XP")
                        GameText(petEffectText(selected, pet.level), size = 15.sp, color = SK.GoldLight, align = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GameButton(stringResource(R.string.feed), { onFeed(selected) }, enabled = state.petTreats > 0 && pet.level < GameBalanceConfig.PET_MAX_LEVEL, color = SK.Orange, modifier = Modifier.weight(1f).testTag("feed"), leading = { TreatIcon(22.dp) })
                            GameButton(stringResource(if (state.activePet == selected) R.string.active else R.string.activate), { onActivate(selected) }, enabled = state.activePet != selected, modifier = Modifier.weight(1f).testTag("activate"))
                        }
                        GameText(stringResource(R.string.treat_hint, GameBalanceConfig.TREAT_XP), size = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun petDescription(type: PetType) = stringResource(when (type) { PetType.FOX -> R.string.pet_fox_desc; PetType.DRAGON -> R.string.pet_dragon_desc; PetType.RACCOON -> R.string.pet_raccoon_desc; PetType.PHOENIX -> R.string.pet_phoenix_desc })

@Composable
fun petEffectText(type: PetType, level: Int): String {
    val pct = (GameBalanceConfig.petEffect(type, level) * 100).roundToInt()
    return stringResource(when (type) { PetType.FOX -> R.string.pet_fox_effect; PetType.DRAGON -> R.string.pet_dragon_effect; PetType.RACCOON -> R.string.pet_raccoon_effect; PetType.PHOENIX -> R.string.pet_phoenix_effect }, pct)
}

// =================================================================== CARDS
@Composable
fun CardsScreen(state: GameState, t: Float, onClaim: (Int) -> Unit, onBack: () -> Unit) {
    if (state.level < GameBalanceConfig.CARDS_UNLOCK_LEVEL && state.cards.isEmpty()) {
        LockedScreen(stringResource(R.string.menu_cards), GameBalanceConfig.CARDS_UNLOCK_LEVEL, onBack, t) { CardView(8, 0, t, width = 140.dp) }
        return
    }
    var set by rememberSaveable { mutableIntStateOf(0) }
    var zoom by remember { mutableStateOf<Int?>(null) }
    ScreenScaffold(stringResource(R.string.card_album), onBack, trailing = { GameText("${state.cardsCollected}/${Cards.TOTAL}", size = 15.sp) }) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in 0 until GameBalanceConfig.CARD_SETS) {
                    val unlocked = state.level >= GameBalanceConfig.cardSetUnlockLevel(s)
                    val have = Cards.cardsOfSet(s).count { (state.cards[it] ?: 0) > 0 }
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (s == set) SK.Gold else SK.Panel).border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { set = s }.padding(horizontal = 12.dp, vertical = 6.dp).testTag("set_$s")) {
                        GameText(stringResource(setTitle(s)) + if (unlocked) " $have/9" else " 🔒", size = 13.sp, color = if (s == set) SK.TextDark else Color.White)
                    }
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val unlocked = state.level >= GameBalanceConfig.cardSetUnlockLevel(set)
                if (!unlocked) GameText(stringResource(R.string.set_unlocks_at, GameBalanceConfig.cardSetUnlockLevel(set)), size = 14.sp, color = SK.GoldLight)
                val cards = Cards.cardsOfSet(set)
                cards.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                        row.forEach { id ->
                            val count = state.cards[id] ?: 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CardView(id, count, t, Modifier.clickable(enabled = count > 0) { zoom = id }.testTag("card_$id"), width = 100.dp)
                                GameText(rarityName(Cards.rarity(id)), size = 10.sp, color = rarityColor(Cards.rarity(id)))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Panel(Modifier.fillMaxWidth()) {
                    val have = cards.count { (state.cards[it] ?: 0) > 0 }
                    GameText(stringResource(R.string.set_reward), size = 17.sp, color = SK.GoldLight)
                    ProgressBar(have / 9f, Modifier.fillMaxWidth().padding(vertical = 6.dp), label = "$have / 9", height = 18.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RewardRow({ EnergyIcon(24.dp) }, "${GameBalanceConfig.setRewardSpins(set)}")
                        RewardRow({ CoinIcon(24.dp) }, formatNumber(GameBalanceConfig.coins(state.level, GameBalanceConfig.setRewardCoinsU(set))))
                        RewardRow({ StarIcon(20.dp) }, "${GameBalanceConfig.setRewardPetXp(set)} XP")
                        val (b, tr) = chestColors(GameBalanceConfig.setRewardChest(set))
                        ChestIcon(28.dp, body = b, trim = tr)
                    }
                    val claimed = set in state.claimedSets
                    GameButton(
                        stringResource(if (claimed) R.string.claimed else R.string.claim_reward), { onClaim(set) },
                        enabled = !claimed && have == 9, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("claim_set")
                    )
                }
            }
        }
        zoom?.let { id ->
            val flip = remember(id) { Animatable(0f) }
            LaunchedEffect(id) { flip.animateTo(1f, tween(500)) }
            GameDialog({ zoom = null }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CardView(id, state.cards[id] ?: 0, t, width = 220.dp, flip = flip.value)
                    Spacer(Modifier.height(10.dp))
                    GameText(rarityName(Cards.rarity(id)), size = 18.sp, color = rarityColor(Cards.rarity(id)))
                    GameText(stringResource(R.string.owned_times, state.cards[id] ?: 0), size = 14.sp)
                }
            }
        }
    }
}

fun setTitle(set: Int) = listOf(R.string.set_0, R.string.set_1, R.string.set_2, R.string.set_3, R.string.set_4, R.string.set_5)[set]

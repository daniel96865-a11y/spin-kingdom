package de.danielgrebe.spinkingdom.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.danielgrebe.spinkingdom.BuildConfig
import de.danielgrebe.spinkingdom.R
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.config.GameEvent
import de.danielgrebe.spinkingdom.config.LevelConfig
import de.danielgrebe.spinkingdom.domain.Cards
import de.danielgrebe.spinkingdom.domain.ProductKind
import de.danielgrebe.spinkingdom.domain.ShopProduct
import de.danielgrebe.spinkingdom.game.GameEngine
import de.danielgrebe.spinkingdom.models.ChestType
import de.danielgrebe.spinkingdom.models.GameState
import de.danielgrebe.spinkingdom.models.Settings
import de.danielgrebe.spinkingdom.notifications.Notifications
import de.danielgrebe.spinkingdom.ui.components.GameButton
import de.danielgrebe.spinkingdom.ui.components.GameDialog
import de.danielgrebe.spinkingdom.ui.components.GameText
import de.danielgrebe.spinkingdom.ui.components.Panel
import de.danielgrebe.spinkingdom.ui.components.RewardRow
import de.danielgrebe.spinkingdom.ui.components.ScreenScaffold
import de.danielgrebe.spinkingdom.ui.components.SectionTitle
import de.danielgrebe.spinkingdom.ui.components.formatNumber
import de.danielgrebe.spinkingdom.ui.draw.AVATAR_COUNT
import de.danielgrebe.spinkingdom.ui.draw.Avatar
import de.danielgrebe.spinkingdom.ui.draw.ChestIcon
import de.danielgrebe.spinkingdom.ui.draw.CoinIcon
import de.danielgrebe.spinkingdom.ui.draw.EnergyIcon
import de.danielgrebe.spinkingdom.ui.draw.MenuIcon
import de.danielgrebe.spinkingdom.ui.draw.MenuIconView
import de.danielgrebe.spinkingdom.ui.draw.StarIcon
import de.danielgrebe.spinkingdom.ui.draw.chestColors
import de.danielgrebe.spinkingdom.ui.theme.SK
import java.text.DateFormat
import java.util.Date

// =================================================================== PROFILE
@Composable
fun ProfileScreen(state: GameState, onSave: (String, Int) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf(state.playerName) }
    var avatar by remember { mutableIntStateOf(state.avatar) }
    val guest = stringResource(R.string.guest)
    ScreenScaffold(stringResource(R.string.menu_profile), onBack = { onSave(name, avatar); onBack() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(avatar, 120.dp)
            GameText(name.ifBlank { guest }, size = 26.sp)
            GameText(stringResource(R.string.level_title, state.level, levelName(LevelConfig.level(state.level))), size = 14.sp, color = SK.GoldLight)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until AVATAR_COUNT) Box(Modifier.clip(CircleShape).border(if (i == avatar) 3.dp else 0.dp, SK.Gold, CircleShape).clickable { avatar = i }) { Avatar(i, 38.dp) }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(16) }, singleLine = true, label = { Text(stringResource(R.string.player_name)) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SK.Gold, unfocusedBorderColor = Color.White.copy(alpha = 0.6f), focusedLabelColor = SK.Gold, unfocusedLabelColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            GameButton(stringResource(R.string.save), { onSave(name, avatar) }, Modifier.fillMaxWidth().padding(top = 8.dp), height = 46.dp)
            Spacer(Modifier.height(12.dp))
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.statistics))
                StatRow({ StarIcon(22.dp) }, stringResource(R.string.stars), "${state.stars}")
                StatRow({ CoinIcon(22.dp) }, stringResource(R.string.coins), formatNumber(state.coins))
                StatRow({ CoinIcon(22.dp) }, stringResource(R.string.stat_coins_earned), formatNumber(state.stats.coinsEarned))
                StatRow({ MenuIconView(MenuIcon.HAMMER, 22.dp) }, stringResource(R.string.stat_attacks_won), "${state.stats.attacksWon}")
                StatRow({ MenuIconView(MenuIcon.HAMMER, 22.dp) }, stringResource(R.string.stat_attacks_blocked), "${state.stats.attacksBlocked}")
                StatRow({ MenuIconView(MenuIcon.SWORD, 22.dp) }, stringResource(R.string.stat_raids), "${state.stats.raidsDone}")
                StatRow({ MenuIconView(MenuIcon.CARDS, 22.dp) }, stringResource(R.string.stat_cards), "${state.cardsCollected} / ${Cards.TOTAL}")
                StatRow({ EnergyIcon(22.dp) }, stringResource(R.string.stat_spins), "${state.stats.totalSpins}")
                StatRow({ MenuIconView(MenuIcon.HAMMER, 22.dp) }, stringResource(R.string.stat_upgrades), "${state.stats.buildingsUpgraded}")
                StatRow({ MenuIconView(MenuIcon.TROPHY, 22.dp) }, stringResource(R.string.stat_levels), "${state.stats.levelsCompleted}")
                StatRow({ MenuIconView(MenuIcon.GIFT, 22.dp) }, stringResource(R.string.stat_chests), "${state.stats.chestsOpened}")
                StatRow({ MenuIconView(MenuIcon.WHEEL, 22.dp) }, stringResource(R.string.stat_jackpots), "${state.stats.jackpots}")
                StatRow({ CoinIcon(22.dp) }, stringResource(R.string.stat_biggest_win), formatNumber(state.stats.biggestWin))
            }
        }
    }
}

@Composable
private fun StatRow(icon: @Composable () -> Unit, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(8.dp))
        GameText(label, Modifier.weight(1f), size = 14.sp)
        GameText(value, size = 15.sp, color = SK.GoldLight)
    }
}

// =================================================================== SHOP
@Composable
fun ShopScreen(state: GameState, products: List<ShopProduct>, busy: Boolean, onBuy: (ShopProduct) -> Unit, onWatchAd: () -> Unit, onBack: () -> Unit) {
    var confirm by remember { mutableStateOf<ShopProduct?>(null) }
    ScreenScaffold(stringResource(R.string.menu_shop), onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SK.Red).padding(8.dp), contentAlignment = Alignment.Center) {
                GameText(stringResource(R.string.shop_test_banner), size = 14.sp, align = TextAlign.Center)
            }
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth(), color = SK.Purple.copy(alpha = 0.8f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MenuIconView(MenuIcon.TV, 44.dp)
                    Spacer(Modifier.width(10.dp))
                    GameText(stringResource(R.string.watch_ad_for_spins, GameBalanceConfig.AD_REWARD_SPINS), Modifier.weight(1f), size = 15.sp)
                    GameButton(stringResource(R.string.watch), onWatchAd, height = 44.dp, textSize = 14.sp, modifier = Modifier.testTag("shop_ad"))
                }
            }
            listOf(ProductKind.OFFER to R.string.shop_offers, ProductKind.SPINS to R.string.shop_spins, ProductKind.COINS to R.string.shop_coins, ProductKind.CHEST to R.string.shop_chests).forEach { (kind, title) ->
                SectionTitle(stringResource(title))
                products.filter { it.kind == kind }.forEach { p ->
                    Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp), color = if (p.highlight) SK.GoldDark.copy(alpha = 0.9f) else SK.Panel, padding = 10.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                GameText(productTitle(p), size = 16.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (p.spins > 0) RewardRow({ EnergyIcon(22.dp) }, "${p.spins}")
                                    if (p.coinsU > 0) RewardRow({ CoinIcon(22.dp) }, formatNumber(GameBalanceConfig.coins(state.level, p.coinsU)))
                                    p.chest?.let { val (b, tr) = chestColors(it); RewardRow({ ChestIcon(26.dp, body = b, trim = tr) }, if (p.spins == 0 && p.coinsU == 0.0) chestName(it) else "+1") }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                GameText(p.priceLabel, size = 13.sp, color = Color.White.copy(alpha = 0.8f))
                                GameButton(stringResource(R.string.test_purchase), { confirm = p }, enabled = !busy, height = 42.dp, textSize = 13.sp, modifier = Modifier.testTag("buy_${p.id}"),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            GameText(stringResource(R.string.shop_footer), size = 11.sp, color = Color.White.copy(alpha = 0.6f), align = TextAlign.Center)
        }
        confirm?.let { p ->
            GameDialog({ confirm = null }) {
                Panel(Modifier.fillMaxWidth()) {
                    GameText(stringResource(R.string.confirm_test_purchase), Modifier.fillMaxWidth(), size = 20.sp, align = TextAlign.Center)
                    GameText(productTitle(p) + " · " + p.priceLabel, Modifier.fillMaxWidth(), size = 15.sp, align = TextAlign.Center, color = SK.GoldLight)
                    GameText(stringResource(R.string.no_real_money), Modifier.fillMaxWidth().padding(vertical = 8.dp), size = 14.sp, align = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameButton(stringResource(R.string.cancel), { confirm = null }, Modifier.weight(1f), color = Color(0xFF6B6680))
                        GameButton(stringResource(R.string.test_purchase), { onBuy(p); confirm = null }, Modifier.weight(1f).testTag("confirm_buy"))
                    }
                }
            }
        }
    }
}

@Composable
fun productTitle(p: ShopProduct): String = stringResource(
    when (p.id) {
        "offer_starter" -> R.string.p_offer_starter
        "offer_king" -> R.string.p_offer_king
        "spins_small" -> R.string.p_spins_small
        "spins_medium" -> R.string.p_spins_medium
        "spins_large" -> R.string.p_spins_large
        "coins_small" -> R.string.p_coins_small
        "coins_large" -> R.string.p_coins_large
        "chest_gold" -> R.string.p_chest_gold
        "chest_royal" -> R.string.p_chest_royal
        else -> R.string.p_chest_legendary
    }
)

// =================================================================== SETTINGS
val LANGUAGES = listOf("de" to "Deutsch", "en" to "English", "pl" to "Polski", "fr" to "Français", "es" to "Español")

@Composable
fun SettingsScreen(state: GameState, onUpdate: ((Settings) -> Settings) -> Unit, onLanguage: (String) -> Unit, onReset: () -> Unit, onDebug: () -> Unit, onBack: () -> Unit) {
    val s = state.settings
    var showPrivacy by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }
    var versionTaps by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onUpdate { it.copy(notifications = granted) }
    }
    ScreenScaffold(stringResource(R.string.menu_settings), onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.audio_and_feel))
                ToggleRow(stringResource(R.string.music), s.music, "toggle_music") { v -> onUpdate { it.copy(music = v) } }
                ToggleRow(stringResource(R.string.sound_effects), s.sfx, "toggle_sfx") { v -> onUpdate { it.copy(sfx = v) } }
                ToggleRow(stringResource(R.string.vibration), s.vibration, "toggle_vibration") { v -> onUpdate { it.copy(vibration = v) } }
                ToggleRow(stringResource(R.string.notifications), s.notifications, "toggle_notifications") { v ->
                    if (v && Build.VERSION.SDK_INT >= 33 && !Notifications.hasPermission(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else onUpdate { it.copy(notifications = v) }
                }
                GameText(stringResource(R.string.notifications_hint), size = 11.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.language))
                Row(Modifier.fillMaxWidth().clickable { showLang = true }.padding(vertical = 6.dp).testTag("language_row"), verticalAlignment = Alignment.CenterVertically) {
                    GameText(LANGUAGES.firstOrNull { it.first == currentLanguage(s.language) }?.second ?: "Deutsch", Modifier.weight(1f), size = 16.sp)
                    GameText("›", size = 22.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.savegame))
                InfoRow(stringResource(R.string.player_id), state.playerId)
                InfoRow(stringResource(R.string.started_on), DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(state.createdAt)))
                InfoRow(stringResource(R.string.save_location), stringResource(R.string.save_location_value))
                Spacer(Modifier.height(6.dp))
                GameButton(stringResource(R.string.privacy), { showPrivacy = true }, Modifier.fillMaxWidth(), color = SK.Blue, height = 46.dp, textSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                GameButton(stringResource(R.string.reset_game), { showReset = true }, Modifier.fillMaxWidth().testTag("reset_button"), color = SK.Red, height = 46.dp, textSize = 15.sp)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().clickable {
                versionTaps++
                if (versionTaps >= 5 && BuildConfig.DEBUG_MENU_ENABLED) { versionTaps = 0; onDebug() }
            }.padding(12.dp).testTag("version"), contentAlignment = Alignment.Center) {
                GameText(stringResource(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), size = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
        if (showPrivacy) GameDialog({ showPrivacy = false }) {
            Panel(Modifier.fillMaxWidth()) {
                GameText(stringResource(R.string.privacy), size = 22.sp)
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    GameText(stringResource(R.string.privacy_text), size = 14.sp, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
                GameButton(stringResource(R.string.ok), { showPrivacy = false }, Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
        if (showReset) GameDialog({ showReset = false }) {
            Panel(Modifier.fillMaxWidth()) {
                GameText(stringResource(R.string.reset_game), size = 22.sp)
                GameText(stringResource(R.string.reset_confirm), size = 15.sp, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameButton(stringResource(R.string.cancel), { showReset = false }, Modifier.weight(1f), color = Color(0xFF6B6680))
                    GameButton(stringResource(R.string.reset), { showReset = false; onReset() }, Modifier.weight(1f).testTag("reset_confirm"), color = SK.Red)
                }
            }
        }
        if (showLang) GameDialog({ showLang = false }) {
            Panel(Modifier.fillMaxWidth()) {
                GameText(stringResource(R.string.language), size = 22.sp)
                LANGUAGES.forEach { (code, label) ->
                    Row(Modifier.fillMaxWidth().clickable { showLang = false; onLanguage(code) }.padding(vertical = 10.dp).testTag("lang_$code"), verticalAlignment = Alignment.CenterVertically) {
                        GameText(label, Modifier.weight(1f), size = 17.sp, color = if (code == currentLanguage(s.language)) SK.Gold else Color.White)
                        if (code == currentLanguage(s.language)) GameText("✓", size = 18.sp, color = SK.Gold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        GameText(label, Modifier.weight(1f), size = 16.sp)
        Switch(value, onChange, modifier = Modifier.testTag(tag), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SK.Green, uncheckedTrackColor = Color(0xFF4A4460)))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        GameText(label, Modifier.weight(1f), size = 14.sp)
        GameText(value, size = 14.sp, color = SK.GoldLight)
    }
}

// =================================================================== DEBUG
@Composable
fun DebugScreen(
    state: GameState,
    events: List<GameEvent>,
    onAction: ((GameState) -> GameState) -> Unit,
    onCompleteLevel: () -> Unit,
    onChest: (ChestType) -> Unit,
    onForceEvent: (String?) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onLuckyBoost: () -> Unit = {},
    onSpinJackpot: () -> Unit = {}
) {
    var level by remember { mutableIntStateOf(state.level) }
    ScreenScaffold(stringResource(R.string.debug_menu), onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GameText(stringResource(R.string.debug_hint), size = 12.sp, color = SK.GoldLight)
            DebugRow(stringResource(R.string.debug_coins)) { onAction { GameEngine.Debug.addCoins(it, GameBalanceConfig.levelTotalCost(it.level)) } }
            DebugRow(stringResource(R.string.debug_spins)) { onAction { GameEngine.Debug.addSpins(it, 500) } }
            DebugRow(stringResource(R.string.debug_shields)) { onAction { GameEngine.Debug.addShields(it) } }
            DebugRow(stringResource(R.string.debug_treats)) { onAction { GameEngine.Debug.addTreats(it) } }
            DebugRow(stringResource(R.string.debug_lucky_boost)) { onLuckyBoost() }
            DebugRow(stringResource(R.string.debug_spin_jackpot)) { onSpinJackpot() }
            DebugRow(stringResource(R.string.debug_cards)) { onAction { GameEngine.Debug.unlockAllCards(it) } }
            DebugRow(stringResource(R.string.debug_finish_buildings)) { onAction { GameEngine.Debug.finishBuildingsExceptLast(it) } }
            DebugRow(stringResource(R.string.debug_complete_level)) { onCompleteLevel() }
            DebugRow(stringResource(R.string.debug_reset_daily)) { onAction { GameEngine.Debug.resetDaily(it) } }
            DebugRow(stringResource(R.string.debug_attack)) { onAction { GameEngine.Debug.pendingAttack(it) } }
            DebugRow(stringResource(R.string.debug_raid)) { onAction { GameEngine.Debug.pendingRaid(it) } }
            DebugRow(stringResource(R.string.debug_damage)) { onAction { GameEngine.Debug.damageBuilding(it) } }
            DebugRow(stringResource(R.string.debug_bot_attack)) { onAction { it.copy(lastBotCheckMillis = it.lastBotCheckMillis - GameBalanceConfig.BOT_ATTACK_INTERVAL_MS * 12) } }
            Panel(Modifier.fillMaxWidth()) {
                GameText(stringResource(R.string.debug_level, level), size = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GameButton("-10", { level = (level - 10).coerceAtLeast(1) }, Modifier.weight(1f), height = 42.dp, color = SK.Blue, textSize = 14.sp)
                    GameButton("-1", { level = (level - 1).coerceAtLeast(1) }, Modifier.weight(1f), height = 42.dp, color = SK.Blue, textSize = 14.sp)
                    GameButton("+1", { level = (level + 1).coerceAtMost(LevelConfig.MAX_LEVEL) }, Modifier.weight(1f), height = 42.dp, color = SK.Blue, textSize = 14.sp)
                    GameButton("+10", { level = (level + 10).coerceAtMost(LevelConfig.MAX_LEVEL) }, Modifier.weight(1f), height = 42.dp, color = SK.Blue, textSize = 14.sp)
                }
                GameButton(stringResource(R.string.debug_set_level), { onAction { GameEngine.Debug.setLevel(it, level) } }, Modifier.fillMaxWidth().padding(top = 6.dp), height = 44.dp, textSize = 15.sp)
            }
            Panel(Modifier.fillMaxWidth()) {
                GameText(stringResource(R.string.debug_chests), size = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChestType.entries.forEach { c -> GameButton(chestName(c).take(6), { onChest(c) }, Modifier.weight(1f), height = 40.dp, textSize = 10.sp, color = SK.Orange, contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)) }
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                GameText(stringResource(R.string.debug_events), size = 16.sp)
                events.forEach { e ->
                    Row(Modifier.fillMaxWidth().clickable { onForceEvent(if (state.debugForcedEvent == e.id) null else e.id) }.padding(vertical = 6.dp)) {
                        GameText(eventName(e.id), Modifier.weight(1f), size = 15.sp)
                        if (state.debugForcedEvent == e.id) GameText("✓", size = 16.sp, color = SK.Gold)
                    }
                }
            }
            DebugRow(stringResource(R.string.reset_game), SK.Red) { onReset() }
        }
    }
}

@Composable
private fun DebugRow(label: String, color: Color = SK.PanelLight, onClick: () -> Unit) =
    GameButton(label, onClick, Modifier.fillMaxWidth(), color = color, height = 46.dp, textSize = 15.sp)


/** The language the UI is really shown in (per-app locale / system), falling back to the saved setting. */
@Composable
fun currentLanguage(saved: String): String {
    val lang = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language
    return if (lang != null && LANGUAGES.any { it.first == lang }) lang else saved
}

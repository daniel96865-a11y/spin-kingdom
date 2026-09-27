package de.danielgrebe.spinkingdom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.danielgrebe.spinkingdom.AppContainer
import de.danielgrebe.spinkingdom.audio.AudioManager
import de.danielgrebe.spinkingdom.audio.Sfx
import de.danielgrebe.spinkingdom.config.EventConfig
import de.danielgrebe.spinkingdom.config.EventModifiers
import de.danielgrebe.spinkingdom.config.GameEvent
import de.danielgrebe.spinkingdom.domain.AdResult
import de.danielgrebe.spinkingdom.domain.AdsRepository
import de.danielgrebe.spinkingdom.domain.BillingRepository
import de.danielgrebe.spinkingdom.domain.ChestContents
import de.danielgrebe.spinkingdom.domain.ClockGuard
import de.danielgrebe.spinkingdom.domain.GameStateRepository
import de.danielgrebe.spinkingdom.domain.LeaderboardEntry
import de.danielgrebe.spinkingdom.domain.LeaderboardRepository
import de.danielgrebe.spinkingdom.domain.Opponent
import de.danielgrebe.spinkingdom.domain.OpponentRepository
import de.danielgrebe.spinkingdom.domain.OutcomeType
import de.danielgrebe.spinkingdom.domain.PurchaseResult
import de.danielgrebe.spinkingdom.domain.ShopProduct
import de.danielgrebe.spinkingdom.domain.SpinRegen
import de.danielgrebe.spinkingdom.domain.TimeSource
import de.danielgrebe.spinkingdom.game.ActionResult
import de.danielgrebe.spinkingdom.game.Ctx
import de.danielgrebe.spinkingdom.game.GameEffect
import de.danielgrebe.spinkingdom.game.GameEngine
import de.danielgrebe.spinkingdom.game.GameError
import de.danielgrebe.spinkingdom.game.RaidPrizeKind
import de.danielgrebe.spinkingdom.models.ChestType
import de.danielgrebe.spinkingdom.models.GameState
import de.danielgrebe.spinkingdom.models.LogEntry
import de.danielgrebe.spinkingdom.models.PetType
import de.danielgrebe.spinkingdom.models.Settings
import de.danielgrebe.spinkingdom.models.SlotSymbol
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.random.Random

/** Popups shown one after another on top of every screen. */
sealed interface Overlay {
    data class BigWin(val coins: Long, val spins: Int, val type: OutcomeType, val multiplier: Int, val canDouble: Boolean) : Overlay
    data class Chest(val contents: ChestContents, val newCards: List<Int>) : Overlay
    data class LevelComplete(val level: Int, val coins: Long, val spins: Int, val chest: ChestType) : Overlay
    data class PetLevelUp(val pet: PetType, val level: Int) : Overlay
    data class SetComplete(val set: Int) : Overlay
    data class BotLog(val entries: List<LogEntry>) : Overlay
    data class Reward(val titleRes: Int, val coins: Long, val spins: Int) : Overlay
    data class Message(val titleRes: Int, val textRes: Int) : Overlay
}

data class SpinAnim(val id: Long, val reels: List<SlotSymbol>, val outcome: OutcomeType, val multiplier: Int, val coins: Long, val spins: Int, val free: Boolean)

data class RaidSession(val opponent: Opponent, val board: List<RaidPrizeKind>, val picks: List<Int> = emptyList(), val multiplier: Int, val finished: Boolean = false, val coins: Long = 0, val spins: Int = 0)

class GameViewModel(
    private val repo: GameStateRepository,
    private val opponents: OpponentRepository,
    private val leaderboardRepo: LeaderboardRepository,
    val billing: BillingRepository,
    val ads: AdsRepository,
    private val time: TimeSource,
    private val events: List<GameEvent>,
    private val audio: AudioManager?,
    private val random: Random = Random.Default,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) : ViewModel() {

    private var truth = GameState()
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _now = MutableStateFlow(time.wallMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val _spinAnim = MutableStateFlow<SpinAnim?>(null)
    val spinAnim: StateFlow<SpinAnim?> = _spinAnim.asStateFlow()
    private var heldEffects: List<GameEffect> = emptyList()

    private val _overlays = MutableStateFlow<List<Overlay>>(emptyList())
    val overlays: StateFlow<List<Overlay>> = _overlays.asStateFlow()

    private val _clockTampered = MutableStateFlow(false)
    val clockTampered: StateFlow<Boolean> = _clockTampered.asStateFlow()

    private val _mods = MutableStateFlow(EventModifiers.NONE)
    val mods: StateFlow<EventModifiers> = _mods.asStateFlow()

    private val _nav = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val nav: SharedFlow<String> = _nav.asSharedFlow()

    private val _toast = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val toast: SharedFlow<Int> = _toast.asSharedFlow()

    private val _attackTarget = MutableStateFlow<Opponent?>(null)
    val attackTarget: StateFlow<Opponent?> = _attackTarget.asStateFlow()

    private val _raid = MutableStateFlow<RaidSession?>(null)
    val raid: StateFlow<RaidSession?> = _raid.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val saves = Channel<GameState>(Channel.CONFLATED)
    private var lastDoubleable: Pair<Long, Int>? = null

    init {
        viewModelScope.launch { for (s in saves) runCatching { repo.save(s) } }
        viewModelScope.launch {
            var s = runCatching { repo.load() }.getOrDefault(GameState())
            if (s.playerId.isEmpty()) s = GameEngine.newGame(UUID.randomUUID().toString().take(8).uppercase(), time.wallMillis())
            truth = s
            val c = ctx()
            truth = GameEngine.tick(truth, c)
            publish(); save()
            applySettings(truth.settings)
            _loaded.value = true
            runForegroundChecks()
            while (true) {
                delay(1000)
                tickOnce()
            }
        }
    }

    // ------------------------------------------------------------------ core plumbing
    private fun ctx(): Ctx {
        val obs = ClockGuard.observe(truth.clock, time.wallMillis(), time.elapsedRealtime())
        truth = truth.copy(clock = obs.clock)
        _clockTampered.value = obs.tampered
        val now = obs.trustedNow
        val date = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate()
        val today = date.toEpochDay()
        val week = today - (date.dayOfWeek.value - 1)
        val mods = EventConfig.modifiers(events, date, truth.debugForcedEvent)
        _mods.value = mods
        _now.value = now
        return Ctx(now, today, week, mods, random)
    }

    private fun publish() { if (_spinAnim.value == null) _state.value = truth }
    private fun save() { saves.trySend(truth) }

    private fun commit(r: ActionResult, showEffects: Boolean = true): ActionResult {
        if (r.error != null) { onError(r.error); return r }
        truth = r.state
        publish(); save()
        if (showEffects) showEffects(r.effects)
        return r
    }

    private fun onError(e: GameError) {
        audio?.play(Sfx.FAIL)
        _toast.tryEmit(
            when (e) {
                GameError.NOT_ENOUGH_SPINS -> de.danielgrebe.spinkingdom.R.string.err_no_spins
                GameError.NOT_ENOUGH_COINS -> de.danielgrebe.spinkingdom.R.string.err_no_coins
                GameError.MAX_STAGE -> de.danielgrebe.spinkingdom.R.string.err_max_stage
                GameError.ALREADY_CLAIMED -> de.danielgrebe.spinkingdom.R.string.err_already_claimed
                GameError.NOT_AVAILABLE -> de.danielgrebe.spinkingdom.R.string.err_not_available
                GameError.CLOCK_TAMPERED -> de.danielgrebe.spinkingdom.R.string.err_clock
                GameError.NO_TREATS -> de.danielgrebe.spinkingdom.R.string.err_no_treats
            }
        )
    }

    private fun tickOnce() {
        if (_spinAnim.value != null) { _now.value = time.wallMillis(); return }
        val before = truth
        val c = ctx()
        truth = GameEngine.tick(truth, c)
        val changed = truth.copy(clock = before.clock) != before
        publish()
        if (changed) save()
    }

    /** Shows popups/sounds for engine effects. */
    fun showEffects(effects: List<GameEffect>) {
        val add = mutableListOf<Overlay>()
        for (e in effects) when (e) {
            is GameEffect.ChestOpened -> add += Overlay.Chest(e.contents, e.newCards)
            is GameEffect.LevelCompleted -> { add.add(0, Overlay.LevelComplete(e.completedLevel, e.coins, e.spins, e.chest)); audio?.play(Sfx.LEVEL_COMPLETE); audio?.vibrate(200, true) }
            is GameEffect.PetLevelUp -> add += Overlay.PetLevelUp(e.pet, e.level)
            is GameEffect.SetCompleted -> add += Overlay.SetComplete(e.set)
            is GameEffect.BuildingUpgraded -> { audio?.play(Sfx.UPGRADE); audio?.vibrate(40) }
            is GameEffect.BuildingRepaired -> audio?.play(Sfx.UPGRADE)
            is GameEffect.MissionClaimed -> { audio?.play(Sfx.COIN); add.add(0, Overlay.Reward(de.danielgrebe.spinkingdom.R.string.mission_reward, e.coins, e.spins)) }
            is GameEffect.Purchased -> { audio?.play(Sfx.COIN); add.add(0, Overlay.Reward(de.danielgrebe.spinkingdom.R.string.shop_test_success, e.coins, e.spins)) }
            is GameEffect.Rewarded -> { audio?.play(Sfx.COIN); add.add(0, Overlay.Reward(de.danielgrebe.spinkingdom.R.string.reward_title, e.coins, e.spins)) }
            is GameEffect.DailyClaimed -> { audio?.play(Sfx.COIN); if (e.coins > 0 || e.spins > 0) add.add(0, Overlay.Reward(de.danielgrebe.spinkingdom.R.string.daily_title, e.coins, e.spins)) }
            is GameEffect.WheelSpun -> { audio?.play(Sfx.JACKPOT); if (e.coins > 0 || e.spins > 0 || e.treats > 0) add.add(0, Overlay.Reward(de.danielgrebe.spinkingdom.R.string.wheel_title, e.coins, e.spins)) }
            else -> Unit
        }
        if (add.isNotEmpty()) _overlays.value = _overlays.value + add
    }

    fun dismissOverlay() {
        val list = _overlays.value
        if (list.isEmpty()) return
        val first = list.first()
        if (first is Overlay.BigWin) lastDoubleable = null
        if (first is Overlay.BotLog) { truth = GameEngine.markLogSeen(truth); publish(); save() }
        _overlays.value = list.drop(1)
    }

    fun sfx(s: Sfx) = audio?.play(s)
    fun vibrate(ms: Long = 30, strong: Boolean = false) = audio?.vibrate(ms, strong)

    // ------------------------------------------------------------------ lifecycle
    fun onForeground() {
        audio?.onForeground()
        if (_loaded.value) runForegroundChecks()
    }

    private fun runForegroundChecks() {
        val c = ctx()
        truth = GameEngine.tick(truth, c)
        truth = GameEngine.simulateBotAttacks(truth, c, opponents.botNames())
        publish(); save()
        val unseen = truth.log.filter { !it.seen }
        if (unseen.isNotEmpty() && _overlays.value.none { it is Overlay.BotLog }) {
            _overlays.value = listOf(Overlay.BotLog(unseen)) + _overlays.value
        }
    }

    fun onBackground(): Long? {
        audio?.onBackground()
        save()
        return if (truth.settings.notifications && truth.introDone) SpinRegen.millisUntilFull(truth, time.wallMillis()) else null
    }

    // ------------------------------------------------------------------ intro
    fun completeIntro(name: String?, avatar: Int) {
        truth = GameEngine.finishIntro(truth, name, avatar, ctx())
        publish(); save()
        audio?.play(Sfx.COIN)
    }

    // ------------------------------------------------------------------ slot machine
    fun selectMultiplier(m: Int) {
        if (_spinAnim.value != null) return
        truth = GameEngine.selectMultiplier(truth, m)
        publish(); save()
        audio?.play(Sfx.CLICK)
    }

    fun spin(forced: List<SlotSymbol>? = null): Boolean {
        if (_spinAnim.value != null || !truth.introDone) return false
        val before = truth
        val c = ctx()
        truth = GameEngine.tick(truth, c)
        val r = GameEngine.spin(truth, c, forced)
        if (r.error != null) { onError(r.error); publish(); return false }
        val spun = r.effects.first() as GameEffect.Spun
        truth = r.state
        save()
        // show the spin cost immediately, everything else after the reels stopped
        _state.value = before.copy(spins = if (spun.freeSpin) before.spins else before.spins - spun.multiplier, clock = truth.clock, lastRegenMillis = truth.lastRegenMillis)
        heldEffects = r.effects.drop(1)
        _spinAnim.value = SpinAnim(System.nanoTime(), spun.reels, spun.outcome.type, spun.multiplier, spun.coins, spun.spins, spun.freeSpin)
        audio?.play(Sfx.SPIN)
        audio?.vibrate(20)
        return true
    }

    /** Called by the slot machine when all reels have stopped. */
    fun onReelsStopped() {
        val anim = _spinAnim.value ?: return
        _spinAnim.value = null
        _state.value = truth
        when (anim.outcome) {
            OutcomeType.JACKPOT -> { audio?.play(Sfx.JACKPOT); audio?.vibrate(300, true) }
            OutcomeType.COIN_TRIPLE -> { audio?.play(Sfx.JACKPOT); audio?.vibrate(120) }
            OutcomeType.ENERGY -> { audio?.play(Sfx.JACKPOT) }
            OutcomeType.SHIELD -> audio?.play(Sfx.SHIELD)
            OutcomeType.ATTACK -> { audio?.play(Sfx.ATTACK); _nav.tryEmit("attack") }
            OutcomeType.RAID -> { audio?.play(Sfx.DIG); _nav.tryEmit("raid") }
            OutcomeType.CHEST -> audio?.play(Sfx.CHEST)
            OutcomeType.COIN_PAIR, OutcomeType.OTHER_PAIR, OutcomeType.SINGLE_COIN, OutcomeType.ENERGY_PAIR -> audio?.play(Sfx.COIN, 0.6f)
            OutcomeType.NOTHING -> Unit
        }
        if (anim.outcome in setOf(OutcomeType.JACKPOT, OutcomeType.COIN_TRIPLE, OutcomeType.ENERGY)) {
            lastDoubleable = anim.coins to anim.spins
            _overlays.value = _overlays.value + Overlay.BigWin(anim.coins, anim.spins, anim.outcome, anim.multiplier, ads.isRewardedAvailable())
        }
        showEffects(heldEffects)
        heldEffects = emptyList()
    }

    // ------------------------------------------------------------------ village
    fun upgrade(index: Int) { commit(GameEngine.upgrade(truth, index, ctx())) }

    // ------------------------------------------------------------------ attack
    fun prepareAttack() {
        if (truth.pendingActions.none { it.kind == "attack" }) { _attackTarget.value = null; return }
        if (_attackTarget.value != null) return
        viewModelScope.launch { _attackTarget.value = opponents.findAttackTarget(truth.level) }
    }

    fun attack(buildingIndex: Int): GameEffect.AttackDone? {
        val opp = _attackTarget.value ?: return null
        val r = commit(GameEngine.resolveAttack(truth, opp, buildingIndex, ctx()), showEffects = false)
        return r.effects.firstOrNull() as? GameEffect.AttackDone
    }

    fun finishAttack() { _attackTarget.value = null }

    // ------------------------------------------------------------------ raid
    fun prepareRaid() {
        if (truth.pendingActions.none { it.kind == "raid" }) { if (_raid.value?.finished != true) _raid.value = null; return }
        if (_raid.value != null && _raid.value?.finished == false) return
        viewModelScope.launch {
            val opp = opponents.findRaidTarget(truth.level)
            _raid.value = RaidSession(opp, GameEngine.rollRaidBoard(random), multiplier = GameEngine.pendingMultiplier(truth, "raid"))
        }
    }

    fun raidPrizeValue(kind: RaidPrizeKind, mult: Int): Pair<Long, Int> = GameEngine.raidPrizeValue(truth, kind, mult, ctx())

    fun dig(spot: Int) {
        val s = _raid.value ?: return
        if (s.finished || spot in s.picks || s.picks.size >= de.danielgrebe.spinkingdom.config.GameBalanceConfig.RAID_PICKS) return
        val picks = s.picks + spot
        audio?.play(Sfx.DIG); audio?.vibrate(35)
        if (picks.size == de.danielgrebe.spinkingdom.config.GameBalanceConfig.RAID_PICKS) {
            val r = commit(GameEngine.completeRaid(truth, s.board, picks, ctx()), showEffects = false)
            val done = r.effects.firstOrNull() as? GameEffect.RaidDone
            _raid.value = s.copy(picks = picks, finished = true, coins = done?.coins ?: 0, spins = done?.spins ?: 0)
            viewModelScope.launch { delay(700); audio?.play(if (done?.jackpot == true) Sfx.JACKPOT else Sfx.COIN) }
        } else {
            _raid.value = s.copy(picks = picks)
        }
    }

    fun finishRaid() { _raid.value = null }

    // ------------------------------------------------------------------ daily bonus / wheel
    fun claimDaily(): Boolean = commit(GameEngine.claimDaily(truth, ctx(), _clockTampered.value)).ok

    /** Returns the chosen wheel segment; rewards are shown by the wheel screen after its animation. */
    fun spinWheel(): Pair<Int, List<GameEffect>>? {
        val r = commit(GameEngine.spinWheel(truth, ctx(), _clockTampered.value), showEffects = false)
        if (!r.ok) return null
        val seg = (r.effects.first() as GameEffect.WheelSpun).segment
        return seg to r.effects
    }

    fun todayEpochDay(): Long = Instant.ofEpochMilli(_now.value).atZone(zone()).toLocalDate().toEpochDay()

    // ------------------------------------------------------------------ missions, cards, pets
    fun claimMission(id: String) { commit(GameEngine.claimMission(truth, id, ctx())) }
    fun claimSet(set: Int) { commit(GameEngine.claimSet(truth, set, ctx())) }
    fun feedPet(type: PetType) {
        val r = commit(GameEngine.feedPet(truth, type))
        if (r.ok) { audio?.play(Sfx.COIN, 0.7f, 1.3f) }
    }
    fun setActivePet(type: PetType) { truth = GameEngine.setActivePet(truth, type); publish(); save(); audio?.play(Sfx.CLICK) }

    // ------------------------------------------------------------------ leaderboard
    fun loadLeaderboard() { viewModelScope.launch { _leaderboard.value = leaderboardRepo.leaderboard(truth, time.wallMillis()) } }

    // ------------------------------------------------------------------ shop & ads
    fun buy(product: ShopProduct) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            when (val res = billing.purchase(product.id)) {
                is PurchaseResult.Success -> commit(GameEngine.grantProduct(truth, res.product, ctx()))
                else -> _toast.tryEmit(de.danielgrebe.spinkingdom.R.string.shop_failed)
            }
            _busy.value = false
        }
    }

    fun watchAdForSpins() {
        if (!ads.isRewardedAvailable()) return
        viewModelScope.launch {
            if (ads.showRewarded() == AdResult.Rewarded) commit(GameEngine.grantAdSpins(truth))
            else _toast.tryEmit(de.danielgrebe.spinkingdom.R.string.ad_cancelled)
        }
    }

    fun doubleLastWin() {
        val win = lastDoubleable ?: return
        lastDoubleable = null
        _overlays.value = _overlays.value.drop(1)
        viewModelScope.launch {
            if (ads.showRewarded() == AdResult.Rewarded) commit(GameEngine.doubleReward(truth, win.first, win.second))
            else _toast.tryEmit(de.danielgrebe.spinkingdom.R.string.ad_cancelled)
        }
    }

    // ------------------------------------------------------------------ profile & settings
    fun updateProfile(name: String, avatar: Int) {
        truth = truth.copy(playerName = name.trim().take(16), isGuest = name.isBlank() && truth.isGuest, avatar = avatar)
        publish(); save()
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        truth = truth.copy(settings = transform(truth.settings))
        applySettings(truth.settings)
        publish(); save()
    }

    private fun applySettings(s: Settings) {
        audio?.let { it.sfxEnabled = s.sfx; it.vibrationEnabled = s.vibration; it.musicEnabled = s.music }
    }

    fun resetGame() {
        viewModelScope.launch {
            repo.reset()
            val keepSettings = truth.settings
            truth = GameEngine.newGame(UUID.randomUUID().toString().take(8).uppercase(), time.wallMillis()).copy(settings = keepSettings)
            _overlays.value = emptyList(); _attackTarget.value = null; _raid.value = null
            publish(); save()
            _nav.tryEmit("intro")
        }
    }

    // ------------------------------------------------------------------ debug menu
    fun debug(action: (GameState) -> GameState) {
        truth = GameEngine.tick(action(truth), ctx())
        publish(); save()
        audio?.play(Sfx.CLICK)
    }

    fun debugForceEvent(id: String?) { truth = truth.copy(debugForcedEvent = id); ctx(); publish(); save() }
    fun debugCompleteLevel() { commit(GameEngine.completeLevel(truth, ctx())) }
    fun debugChest(type: ChestType) { commit(GameEngine.grantChest(truth, type, ctx())) }
    fun allEvents(): List<GameEvent> = events.distinctBy { it.id }

    class Factory(private val c: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(c.gameRepository, c.opponentRepository, c.leaderboardRepository, c.billingRepository, c.adsRepository, c.timeSource, c.events, c.audio) as T
    }
}

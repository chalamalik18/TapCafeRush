package xyz.tapcaferush.jglsrc.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.tapcaferush.jglsrc.TapCafeApp
import xyz.tapcaferush.jglsrc.data.PreferencesRepository

data class GameUiState(
    val level: CafeLevel = CafeLevel.fromId(1),
    val score: Int = 0,
    val currentOrder: Drink = Drink.COFFEE,
    val isVipOrder: Boolean = false,
    val orderProgress: Float = 1f,
    val combo: Int = 0,
    val multiplier: Float = 1f,
    val isRush: Boolean = false,
    val rushTimeLeftMs: Float = 0f,
    val ordersServedSession: Int = 0,
    val peakCombo: Int = 0,
    val rushScoreSession: Int = 0,
    val nextRushInMs: Float = 42_000f,
    val paused: Boolean = false,
    val correctSession: Int = 0,
    val wrongSession: Int = 0,
    val expiredSession: Int = 0,
    val actionBanner: String? = null,
    val isLevelComplete: Boolean = false,
    val feverTimeLeftMs: Float = 0f,
    val tips: Int = 0,
    val trail: List<TrailDot> = emptyList()
)

private data class OrderRoll(val drink: Drink, val isVip: Boolean)

class GameViewModel(
    application: Application,
    private val level: CafeLevel
) : AndroidViewModel(application) {
    private val prefs: PreferencesRepository =
        (application as TapCafeApp).preferencesRepository

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var sessionCommitted = false
    private var bannerJob: Job? = null

    init {
        resetRun()
    }

    fun startLoop() {
        if (loopJob?.isActive == true) return
        loopJob = viewModelScope.launch {
            while (isActive) {
                val s = _state.value
                if (!s.paused) {
                    tick(TICK_MS)
                }
                delay(TICK_MS)
            }
        }
    }

    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    fun resetRun() {
        sessionCommitted = false
        bannerJob?.cancel()
        val roll = rollOrder()
        _state.value = GameUiState(
            level = level,
            currentOrder = roll.drink,
            isVipOrder = roll.isVip,
            nextRushInMs = level.rushIntervalMs
        )
    }

    fun setPaused(value: Boolean) {
        _state.update { it.copy(paused = value) }
    }

    fun onSelect(drink: Drink) {
        val s = _state.value
        if (s.paused || s.isLevelComplete) return
        if (drink == s.currentOrder) {
            val rushMult = if (s.isRush) s.level.rushScoreMult else 1f
            val comboMult =
                (1f + kotlin.math.min(s.combo + 1, 24) * 0.038f).coerceAtMost(1.45f)
            val vipMult = if (s.isVipOrder) VIP_SCORE_MULT else 1f
            val feverMult = if (s.feverTimeLeftMs > 0f) FEVER_SCORE_MULT else 1f
            val mult = comboMult * rushMult * vipMult * feverMult
            val gained = (BASE_SERVE_POINTS * mult).toInt()
            val newScore = s.score + gained
            val newCombo = s.combo + 1
            val newPeak = kotlin.math.max(s.peakCombo, newCombo)
            val newRushScore = if (s.isRush) s.rushScoreSession + gained else s.rushScoreSession
            val newServed = s.ordersServedSession + 1
            val complete = newServed >= s.level.targetOrders
            var feverLeft = s.feverTimeLeftMs
            if (newCombo > 0 && newCombo % FEVER_COMBO_EVERY == 0) {
                feverLeft = (feverLeft + FEVER_BONUS_MS).coerceAtMost(FEVER_CAP_MS)
            }
            val tipGain = kotlin.math.max(1, (gained * TIP_RATE).toInt())
            val roll = if (complete) OrderRoll(s.currentOrder, s.isVipOrder) else rollOrder()
            val trailNext = (s.trail + TrailDot.HIT).takeLast(TRAIL_MAX)
            val feverPulse = newCombo > 0 && newCombo % FEVER_COMBO_EVERY == 0
            val banner = buildString {
                append(if (complete) "Goal reached · level complete!" else "Served · +$gained pts")
                if (!complete) {
                    if (s.isVipOrder) append(" · VIP bonus")
                    if (feverPulse) append(" · Fever charged")
                    if (roll.isVip) append(" · Next: VIP")
                }
            }
            showBanner(banner)
            _state.update {
                it.copy(
                    score = newScore,
                    combo = newCombo,
                    multiplier = mult,
                    currentOrder = if (complete) it.currentOrder else roll.drink,
                    isVipOrder = if (complete) it.isVipOrder else roll.isVip,
                    orderProgress = 1f,
                    ordersServedSession = newServed,
                    peakCombo = newPeak,
                    rushScoreSession = newRushScore,
                    correctSession = it.correctSession + 1,
                    isLevelComplete = complete,
                    paused = complete,
                    feverTimeLeftMs = feverLeft,
                    tips = it.tips + tipGain,
                    trail = trailNext
                )
            }
            if (complete) {
                stopLoop()
                commitSession()
            }
        } else {
            val penalty = s.level.wrongPenalty
            showBanner("Wrong drink · −$penalty pts · combo reset")
            _state.update {
                it.copy(
                    combo = 0,
                    multiplier = 1f,
                    score = (it.score - penalty).coerceAtLeast(0),
                    wrongSession = it.wrongSession + 1,
                    feverTimeLeftMs = 0f,
                    trail = (it.trail + TrailDot.WRONG).takeLast(TRAIL_MAX)
                )
            }
        }
    }

    private fun tick(deltaMs: Long) {
        val delta = deltaMs.toFloat()
        val wasRush = _state.value.isRush
        _state.update { s ->
            if (s.isLevelComplete) return@update s
            var feverLeft = (s.feverTimeLeftMs - delta).coerceAtLeast(0f)
            var rushActive = s.isRush
            var rushLeft = s.rushTimeLeftMs
            var nextRush = s.nextRushInMs
            if (rushActive) {
                rushLeft -= delta
                if (rushLeft <= 0f) {
                    rushActive = false
                    rushLeft = 0f
                    nextRush = s.level.rushIntervalMs
                }
            } else {
                nextRush -= delta
                if (nextRush <= 0f) {
                    rushActive = true
                    rushLeft = s.level.rushDurationMs
                    nextRush = 0f
                }
            }

            val duration = orderDurationMs(s.level, rushActive, s.isVipOrder)
            var progress = s.orderProgress - delta / duration
            var combo = s.combo
            var mult = s.multiplier
            var order = s.currentOrder
            var vip = s.isVipOrder
            var expired = s.expiredSession
            var trail = s.trail
            if (progress <= 0f) {
                showBanner("Too slow · patience drained · combo reset")
                combo = 0
                mult = 1f
                feverLeft = 0f
                val roll = rollOrder()
                order = roll.drink
                vip = roll.isVip
                progress = 1f
                expired += 1
                trail = (trail + TrailDot.SLOW).takeLast(TRAIL_MAX)
            }

            s.copy(
                isRush = rushActive,
                rushTimeLeftMs = rushLeft.coerceAtLeast(0f),
                nextRushInMs = if (rushActive) 0f else nextRush.coerceAtLeast(0f),
                orderProgress = progress.coerceIn(0f, 1f),
                combo = combo,
                multiplier = mult,
                currentOrder = order,
                isVipOrder = vip,
                expiredSession = expired,
                feverTimeLeftMs = feverLeft,
                trail = trail
            )
        }
        if (!wasRush && _state.value.isRush) {
            showBanner("Rush hour · faster tickets · bonus multiplier")
        }
    }

    private fun orderDurationMs(level: CafeLevel, rush: Boolean, vip: Boolean): Float {
        val base = if (rush) level.baseOrderMs * level.rushOrderFactor else level.baseOrderMs
        val withVip = if (vip) base * VIP_PATIENCE_FACTOR else base
        return withVip * PATIENCE_TIME_SCALE
    }

    private fun rollOrder(): OrderRoll {
        return OrderRoll(
            drink = randomDrink(),
            isVip = Random.nextFloat() < level.vipChance
        )
    }

    private fun randomDrink(): Drink {
        return when (Random.nextInt(3)) {
            0 -> Drink.COFFEE
            1 -> Drink.LATTE
            else -> Drink.TEA
        }
    }

    private fun showBanner(text: String) {
        bannerJob?.cancel()
        _state.update { it.copy(actionBanner = text) }
        bannerJob = viewModelScope.launch {
            delay(BANNER_MS)
            _state.update { it.copy(actionBanner = null) }
        }
    }

    fun commitSession() {
        if (sessionCommitted) return
        sessionCommitted = true
        val s = _state.value
        runBlocking {
            prefs.recordLevelSession(
                levelId = level.id,
                sessionScore = s.score,
                servedDelta = s.ordersServedSession,
                peakCombo = s.peakCombo,
                rushScore = s.rushScoreSession,
                correctDelta = s.correctSession,
                wrongDelta = s.wrongSession,
                expiredDelta = s.expiredSession
            )
        }
    }

    override fun onCleared() {
        commitSession()
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val levelId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(GameViewModel::class.java)) {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
            val level = CafeLevel.fromId(levelId)
            return GameViewModel(application, level) as T
        }
    }

    companion object {
        private const val TICK_MS = 48L
        private const val BASE_SERVE_POINTS = 10
        private const val BANNER_MS = 1_800L
        private const val VIP_SCORE_MULT = 1.2f
        private const val VIP_PATIENCE_FACTOR = 0.9f
        private const val FEVER_SCORE_MULT = 1.12f
        private const val FEVER_BONUS_MS = 4_200f
        private const val FEVER_CAP_MS = 11_000f
        private const val FEVER_COMBO_EVERY = 5
        private const val TIP_RATE = 0.16f
        private const val TRAIL_MAX = 6
        private const val PATIENCE_TIME_SCALE = 0.5f
    }
}

package xyz.tapcaferush.jglsrc.ui.screens

import android.app.Application
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import xyz.tapcaferush.jglsrc.game.Drink
import xyz.tapcaferush.jglsrc.game.GameUiState
import xyz.tapcaferush.jglsrc.game.GameViewModel
import xyz.tapcaferush.jglsrc.game.TrailDot
import xyz.tapcaferush.jglsrc.navigation.Routes
import xyz.tapcaferush.jglsrc.TapCafeApp
import xyz.tapcaferush.jglsrc.ui.theme.LevelBackdrops
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(nav: NavHostController, levelId: Int) {
    val app = LocalContext.current.applicationContext as Application
    val prefs = (LocalContext.current.applicationContext as TapCafeApp).preferencesRepository
    val vibrationOn by prefs.isVibrationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val view = LocalView.current
    val vm: GameViewModel = viewModel(
        factory = GameViewModel.Factory(app, levelId)
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.startLoop()
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.stopLoop()
            vm.commitSession()
        }
    }

    val lid = state.level.id
    val nightBar = lid == 5
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Live shift",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (nightBar) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.level.themeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (nightBar) {
                                LevelBackdrops.accentFor(lid)
                            } else {
                                LevelBackdrops.accentFor(lid).copy(alpha = 0.92f)
                            }
                        )
                        Text(
                            text = "Lv${state.level.id} · ${state.level.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (nightBar) {
                                Color.White.copy(alpha = 0.75f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (nightBar) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (nightBar) {
                        Color(0xFF1B263B).copy(alpha = 0.94f)
                    } else {
                        LevelBackdrops.colorsFor(lid).first().copy(alpha = 0.55f)
                    },
                    titleContentColor = if (nightBar) Color.White else MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = if (nightBar) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            DrinkDock(state = state) { drink ->
                if (vibrationOn) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
                vm.onSelect(drink)
            }
        },
        containerColor = LevelBackdrops.colorsFor(lid).first()
    ) { padding ->
        val grad = LevelBackdrops.verticalBrush(lid)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(grad)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoalStrip(state = state)
                RushStripCompact(state = state)
                FeverStrip(state = state)

                AnimatedVisibility(
                    visible = state.actionBanner != null,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(160))
                ) {
                    Surface(
                        tonalElevation = 3.dp,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.actionBanner.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                TrailRow(trail = state.trail)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricChip("Score", state.score.toString(), Modifier.weight(1f))
                    MetricChip("Combo", state.combo.toString(), Modifier.weight(1f))
                    MetricChip("×", String.format("%.2f", state.multiplier), Modifier.weight(1f))
                    MetricChip("Done", "${state.ordersServedSession}", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricChip("Tips", state.tips.toString(), Modifier.weight(1f))
                    MetricChip(
                        title = "Fever",
                        value = if (state.feverTimeLeftMs > 0f) {
                            "${ceil(state.feverTimeLeftMs / 1000.0).toInt()}s"
                        } else {
                            "—"
                        },
                        modifier = Modifier.weight(1f),
                        emphasize = state.feverTimeLeftMs > 0f
                    )
                }

                OrderTicketCard(state = state)
            }
        }
    }

    if (state.isLevelComplete) {
        val lid = state.level.id
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = if (lid < 5) "Level cleared" else "Final level cleared",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You finished this shift.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Score ${state.score} · Tips ${state.tips} · Peak combo ${state.peakCombo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rush pts ${state.rushScoreSession} · Orders ${state.ordersServedSession} / ${state.level.targetOrders}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = {
                            nav.popBackStack(Routes.HOME, inclusive = false)
                        }
                    ) {
                        Text("Main menu")
                    }
                    if (lid < 5) {
                        TextButton(
                            onClick = {
                                val next = lid + 1
                                nav.navigate(Routes.game(next)) {
                                    popUpTo("game/$lid") { inclusive = true }
                                }
                            }
                        ) {
                            Text("Next level")
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun TrailRow(trail: List<TrailDot>) {
    if (trail.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Last serves",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trail.forEach { dot ->
            val (label, tint) = when (dot) {
                TrailDot.HIT -> "✓" to MaterialTheme.colorScheme.primary
                TrailDot.WRONG -> "✗" to MaterialTheme.colorScheme.error
                TrailDot.SLOW -> "⏱" to MaterialTheme.colorScheme.tertiary
            }
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = tint.copy(alpha = 0.16f),
                    disabledLabelColor = tint.copy(alpha = 0.95f)
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = false,
                    borderColor = tint.copy(alpha = 0.35f)
                )
            )
        }
    }
}

@Composable
private fun FeverStrip(state: GameUiState) {
    if (state.feverTimeLeftMs <= 0f) return
    val frac = (state.feverTimeLeftMs / 11_000f).coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1B5E20).copy(alpha = 0.18f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fever · bonus scoring",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = "${ceil(state.feverTimeLeftMs / 1000.0).toInt()}s",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF2E7D32),
                trackColor = Color(0xFF1B5E20).copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun DrinkDock(
    state: GameUiState,
    onDrinkTap: (Drink) -> Unit
) {
    val enabled = !state.paused && !state.isLevelComplete
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Drink bar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.isVipOrder) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("VIP ticket") },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = Color(0xFFFFC107).copy(alpha = 0.35f),
                            disabledLabelColor = Color(0xFF5D4037)
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrinkTile(
                    emoji = "☕",
                    label = "Coffee",
                    modifier = Modifier.weight(1f),
                    container = Color(0xFF5D4037),
                    content = Color(0xFFFFF8E1),
                    enabled = enabled,
                    highlight = state.currentOrder == Drink.COFFEE,
                    onPick = { onDrinkTap(Drink.COFFEE) }
                )
                DrinkTile(
                    emoji = "🥛",
                    label = "Latte",
                    modifier = Modifier.weight(1f),
                    container = Color(0xFF6D4C41),
                    content = Color(0xFFFFFDE7),
                    enabled = enabled,
                    highlight = state.currentOrder == Drink.LATTE,
                    onPick = { onDrinkTap(Drink.LATTE) }
                )
            }
            DrinkTile(
                emoji = "🍵",
                label = "Tea",
                modifier = Modifier.fillMaxWidth(),
                container = Color(0xFF1B5E20),
                content = Color(0xFFE8F5E9),
                enabled = enabled,
                highlight = state.currentOrder == Drink.TEA,
                onPick = { onDrinkTap(Drink.TEA) }
            )
        }
    }
}

@Composable
private fun DrinkTile(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    container: Color,
    content: Color,
    enabled: Boolean,
    highlight: Boolean,
    onPick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val borderMod = if (highlight) {
        Modifier.border(2.5.dp, Color.White.copy(alpha = 0.85f), shape)
    } else {
        Modifier
    }
    Button(
        onClick = onPick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .then(borderMod),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.45f),
            disabledContentColor = content.copy(alpha = 0.55f)
        ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, fontSize = 20.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoalStrip(state: GameUiState) {
    val target = state.level.targetOrders
    val done = state.ordersServedSession.coerceAtMost(target)
    val frac = (done.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = "Goal $done / $target",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.22f)
            )
        }
    }
}

@Composable
private fun RushStripCompact(state: GameUiState) {
    if (state.isRush) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rush · ${ceil(state.rushTimeLeftMs / 1000.0).toInt()}s",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                val rushFrac =
                    (state.rushTimeLeftMs / state.level.rushDurationMs).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { rushFrac },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.25f)
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next rush",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCountdown(state.nextRushInMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val rushCountdownFrac =
                (state.nextRushInMs / state.level.rushIntervalMs).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { rushCountdownFrac },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Card(
        modifier = modifier.height(48.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) {
                Color(0xFF2E7D32).copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (emphasize) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OrderTicketCard(state: GameUiState) {
    val patiencePct = (state.orderProgress * 100f).toInt().coerceIn(0, 100)
    val shape = RoundedCornerShape(18.dp)
    val vipBorder = if (state.isVipOrder) {
        Modifier.border(2.dp, Color(0xFFFFC107), shape)
    } else {
        Modifier
    }
    Card(
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(vipBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AnimatedContent(
                targetState = state.currentOrder to state.isVipOrder,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(140))
                },
                label = "orderAnim"
            ) { (drink, vip) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = drinkEmoji(drink),
                        fontSize = 40.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = drinkTitle(drink),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (vip) {
                                Spacer(modifier = Modifier.padding(4.dp))
                                Surface(
                                    color = Color(0xFFFFC107).copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "VIP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF3E2723),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Patience",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$patiencePct%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (patiencePct <= 28) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            LinearProgressIndicator(
                progress = { state.orderProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(9.dp)
                    .clip(RoundedCornerShape(6.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = if (patiencePct <= 28) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

private fun drinkTitle(drink: Drink): String = when (drink) {
    Drink.COFFEE -> "Velvet espresso"
    Drink.LATTE -> "Cloud latte"
    Drink.TEA -> "Sunrise iced tea"
}

private fun drinkEmoji(drink: Drink): String = when (drink) {
    Drink.COFFEE -> "☕"
    Drink.LATTE -> "🥛"
    Drink.TEA -> "🍵"
}

private fun formatCountdown(ms: Float): String {
    val total = ceil(ms / 1000.0).toInt().coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

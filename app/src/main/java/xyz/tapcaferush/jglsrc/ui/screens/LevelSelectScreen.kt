package xyz.tapcaferush.jglsrc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import xyz.tapcaferush.jglsrc.game.CafeLevel
import xyz.tapcaferush.jglsrc.navigation.Routes
import xyz.tapcaferush.jglsrc.ui.theme.LevelBackdrops

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(nav: NavHostController) {
    var backPressed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Levels") },
                navigationIcon = {
                    IconButton(
                        enabled = !backPressed,
                        onClick = {
                            backPressed = true
                            nav.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Each level has its own look, vibe, and pace. The playfield background matches the theme you pick.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            CafeLevel.ALL.forEach { level ->
                LevelCard(
                    level = level,
                    onPlay = { nav.navigate(Routes.game(level.id)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LevelCard(level: CafeLevel, onPlay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(
                        brush = LevelBackdrops.horizontalBrush(level.id),
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = "LEVEL ${level.id}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = headerTextColor(level.id).copy(alpha = 0.82f)
                    )
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = headerTextColor(level.id)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = headerTextColor(level.id).copy(alpha = 0.18f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = level.themeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor(level.id),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = level.tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoChip("Goal ${level.targetOrders}", Modifier.weight(1f))
                    InfoChip("VIP ${(level.vipChance * 100).toInt()}%", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                PaceMeter(
                    label = "Speed",
                    value = (level.id / CafeLevel.ALL.size.toFloat()).coerceIn(0.18f, 1f),
                    color = LevelBackdrops.accentFor(level.id)
                )
                Spacer(modifier = Modifier.height(8.dp))
                PaceMeter(
                    label = "Rush pressure",
                    value = ((CafeLevel.ALL.size - level.rushIntervalMs / 10_000f) / 3.0f).coerceIn(0.2f, 1f),
                    color = LevelBackdrops.accentFor(level.id)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Patience approx ${(level.baseOrderMs * 0.5f / 1000f).formatOne()}s · Rush every ${(level.rushIntervalMs / 1000f).formatOne()}s · lasts ${(level.rushDurationMs / 1000f).formatOne()}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Start this level", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun PaceMeter(label: String, value: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun headerTextColor(levelId: Int): Color =
    if (levelId >= 5) Color.White else Color(0xFF3E2723)

private fun Float.formatOne(): String = "%.1f".format(this)

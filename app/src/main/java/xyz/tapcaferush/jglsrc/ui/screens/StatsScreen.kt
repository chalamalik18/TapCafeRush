package xyz.tapcaferush.jglsrc.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import xyz.tapcaferush.jglsrc.TapCafeApp
import xyz.tapcaferush.jglsrc.data.LevelStatsSnapshot
import xyz.tapcaferush.jglsrc.game.CafeLevel
import xyz.tapcaferush.jglsrc.ui.theme.LevelBackdrops

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(nav: NavHostController) {
    val repo = (LocalContext.current.applicationContext as TapCafeApp).preferencesRepository
    val all by repo.allLevelsStats.collectAsStateWithLifecycle(
        initialValue = List(5) { LevelStatsSnapshot() }
    )
    val bestAny = all.maxOfOrNull { it.bestScore } ?: 0
    val totalShifts = all.sumOf { it.shiftsPlayed }
    val totalServedAll = all.sumOf { it.totalServed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryStrip(
                    bestAny = bestAny,
                    totalShifts = totalShifts,
                    totalServed = totalServedAll
                )
            }
            items(
                count = CafeLevel.ALL.size,
                key = { idx -> CafeLevel.ALL[idx].id }
            ) { idx ->
                val level = CafeLevel.ALL[idx]
                val stats = all[idx]
                LevelStatsCard(level = level, stats = stats)
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Accuracy uses correct vs wrong taps only. Slow expirations are tracked separately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryStrip(bestAny: Int, totalShifts: Int, totalServed: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Across all levels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatLine("Best score (any level)", bestAny.toString())
            StatLine("Total shifts finished", totalShifts.toString())
            StatLine("Total drinks logged", totalServed.toString())
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun LevelStatsCard(level: CafeLevel, stats: LevelStatsSnapshot) {
    var expanded by remember(level.id) { mutableStateOf(level.id == 1) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Level ${level.id}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = level.themeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LevelBackdrops.accentFor(level.id)
                    )
                    Text(
                        text = "Best score ${stats.bestScore} · ${stats.shiftsPlayed} shifts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailRow("Shifts completed", stats.shiftsPlayed.toString())
                    DetailRow("Best score", stats.bestScore.toString())
                    DetailRow("Total drinks served", stats.totalServed.toString())
                    DetailRow("Best combo streak", stats.bestCombo.toString())
                    DetailRow("Best rush window points", stats.bestRushScore.toString())
                    DetailRow("Correct drink taps", stats.correctTaps.toString())
                    DetailRow("Wrong drink taps", stats.wrongTaps.toString())
                    DetailRow("Orders expired (slow)", stats.expiredOrders.toString())
                    DetailRow(
                        "Tap accuracy",
                        if (stats.correctTaps + stats.wrongTaps == 0) {
                            "—"
                        } else {
                            "%.1f%%".format(stats.accuracyPercent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

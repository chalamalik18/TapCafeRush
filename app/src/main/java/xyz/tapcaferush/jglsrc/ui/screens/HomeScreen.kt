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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import xyz.tapcaferush.jglsrc.navigation.Routes
import xyz.tapcaferush.jglsrc.ui.theme.AmberGlow
import xyz.tapcaferush.jglsrc.ui.theme.CoralAccent
import xyz.tapcaferush.jglsrc.ui.theme.EspressoBrown
import xyz.tapcaferush.jglsrc.ui.theme.LatteCream
import xyz.tapcaferush.jglsrc.ui.theme.MintFoam

@Composable
fun HomeScreen(nav: NavHostController) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            LatteCream,
            MintFoam.copy(alpha = 0.35f),
            AmberGlow.copy(alpha = 0.55f)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        IconButton(
            onClick = { nav.navigate(Routes.SETTINGS) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = EspressoBrown
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 28.dp, horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalCafe,
                        contentDescription = null,
                        tint = CoralAccent,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap Cafe Rush",
                        style = MaterialTheme.typography.displayLarge,
                        color = EspressoBrown,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Five difficulty floors. Each keeps its own stats. Chain serves before the bar empties.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(26.dp))
            Button(
                onClick = { nav.navigate(Routes.LEVELS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EspressoBrown,
                    contentColor = LatteCream
                )
            ) {
                Text(
                    text = "Choose level & play",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { nav.navigate(Routes.STATS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = "Per-level statistics",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

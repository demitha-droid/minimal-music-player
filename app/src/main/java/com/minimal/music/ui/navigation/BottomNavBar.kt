package com.minimal.music.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.music.ui.theme.PrimaryViolet
import com.minimal.music.ui.theme.SurfaceBorder
import com.minimal.music.ui.theme.SurfaceDark
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary

@Composable
fun MinimalBottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(width = 0.5.dp, color = SurfaceBorder)
            .navigationBarsPadding()
            .height(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigate(screen) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    screen.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = screen.title,
                            tint = if (selected) PrimaryViolet else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (selected) TextPrimary else TextMuted,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    )

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 2.dp)
                                .clip(CircleShape)
                                .background(PrimaryViolet)
                        )
                    }
                }
            }
        }
    }
}

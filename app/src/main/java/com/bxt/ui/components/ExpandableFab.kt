package com.bxt.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bxt.ui.theme.LocalDimens

data class FabAction(
    val label: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)

/* Uncontrolled */
@Composable
fun ExpandableFab(
    actions: List<FabAction>,
    modifier: Modifier = Modifier,
    align: Alignment = Alignment.BottomEnd,
    // 👇 Thêm tham số “compact”
    labelTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    labelPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    iconButtonSize: Dp = 40.dp,
    iconSize: Dp = 18.dp,
    useSmallMainFab: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    ExpandableFab(
        actions = actions,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        align = align,
        labelTextStyle = labelTextStyle,
        labelPadding = labelPadding,
        iconButtonSize = iconButtonSize,
        iconSize = iconSize,
        useSmallMainFab = useSmallMainFab
    )
}

/* Controlled */
@Composable
fun ExpandableFab(
    actions: List<FabAction>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    align: Alignment = Alignment.BottomEnd,
    labelTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    labelPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    iconButtonSize: Dp = 40.dp,
    iconSize: Dp = 18.dp,
    useSmallMainFab: Boolean = true
) {
    val d = LocalDimens.current
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "fab_rotation")

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        onExpandedChange(false)
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(align)
                .padding(horizontal = d.pagePadding, vertical = d.pagePadding),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    actions.forEach { action ->
                        AnimatedVisibility(
                            visible = expanded,
                            enter = scaleIn(initialScale = 0.95f) + fadeIn(),
                            exit = scaleOut(targetScale = 0.95f) + fadeOut()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    tonalElevation = 2.dp,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                ) {
                                    Text(
                                        text = action.label,
                                        style = labelTextStyle,                  // 👈 nhỏ hơn
                                        modifier = Modifier.padding(labelPadding) // 👈 padding nhỏ
                                    )
                                }
                                Spacer(Modifier.width(d.rowGap))
                                FilledIconButton(
                                    onClick = { onExpandedChange(false); action.onClick() },
                                    shape = CircleShape,
                                    modifier = Modifier.size(iconButtonSize)     // 👈 nút icon nhỏ
                                ) {
                                    action.icon()
                                }
                            }
                        }
                    }
                }
            }

            // FAB chính (nhỏ gọn)
            if (useSmallMainFab) {
                SmallFloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.rotate(rotation))
                }
            } else {
                FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.rotate(rotation))
                }
            }
        }
    }
}

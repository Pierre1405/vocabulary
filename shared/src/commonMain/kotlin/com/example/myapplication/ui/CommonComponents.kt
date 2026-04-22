package com.example.myapplication.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableGradeCard(
    onGradeSelected: (Int) -> Unit,
    currentGrade: Int? = null,
    content: @Composable () -> Unit
) {
    val revealWidthDp = 200.dp
    val revealWidthPx = with(LocalDensity.current) { revealWidthDp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(revealWidthDp)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..5).forEach { grade ->
                val isSelected = grade == currentGrade
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) gradeColor(grade).copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .clickable {
                            onGradeSelected(grade)
                            scope.launch { offsetX.animateTo(0f) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$grade",
                        style = if (isSelected) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = gradeColor(grade)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newValue = (offsetX.value + delta).coerceIn(-revealWidthPx, 0f)
                            offsetX.snapTo(newValue)
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (offsetX.value < -revealWidthPx / 2) {
                                offsetX.animateTo(-revealWidthPx)
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    }
                )
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}

@Composable
fun ClickableWordText(
    text: String,
    style: TextStyle,
    color: Color,
    onWordClick: (String) -> Unit
) {
    val tokens = text.split(" ")
    val annotated = buildAnnotatedString {
        tokens.forEachIndexed { i, token ->
            val word = token.trimEnd { !it.isLetter() }
            if (word.isNotEmpty()) {
                pushStringAnnotation("WORD", word)
                withStyle(SpanStyle(color = color)) { append(token) }
                pop()
            } else {
                withStyle(SpanStyle(color = color)) { append(token) }
            }
            if (i < tokens.size - 1) append(" ")
        }
    }
    ClickableText(
        text = annotated,
        style = style,
        onClick = { offset ->
            annotated.getStringAnnotations("WORD", offset, offset)
                .firstOrNull()?.let { onWordClick(it.item) }
        }
    )
}

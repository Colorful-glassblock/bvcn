package dev.aaa1115910.bv.player.seekbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SeekBar(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    colors: SliderColors = SliderDefaults.colors(),
    segments: List<SeekBarSegment> = emptyList(),
) {
    val trackWidth = 32f
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(trackWidth.dp)
    ) {
        drawLine(
            color = colors.inactiveTrackColor,
            start = Offset(0f, center.y),
            end = Offset(size.width - 0f, center.y),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = colors.disabledActiveTrackColor,
            start = Offset(0f, center.y),
            end = Offset(size.width * bufferedPercentage / 100, center.y),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = colors.activeTrackColor,
            start = Offset(0f, center.y),
            end = Offset(size.width * (position / duration.toFloat()), center.y),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        // SponsorBlock 等片段标记，画在进度条上层短竖条
        if (segments.isNotEmpty() && duration > 0) {
            val markerWidth = trackWidth * 0.45f
            segments.forEach { segment ->
                val startX = size.width * (segment.startMs.toFloat() / duration)
                val endX = size.width * (segment.endMs.toFloat() / duration)
                val left = startX.coerceIn(0f, size.width)
                val right = endX.coerceIn(0f, size.width)
                if (right > left) {
                    drawLine(
                        color = Color(segment.color),
                        start = Offset(left, center.y - trackWidth / 2f),
                        end = Offset(right, center.y - trackWidth / 2f),
                        strokeWidth = markerWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * 进度条上的片段标记
 *
 * @param color ARGB 颜色
 */
data class SeekBarSegment(
    val startMs: Long,
    val endMs: Long,
    val color: Long
)

@Preview
@Composable
private fun SeekPreview() {
    MaterialTheme {
        SeekBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            duration = 1000,
            position = 300,
            bufferedPercentage = 50
        )
    }
}
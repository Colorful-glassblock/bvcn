package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.util.formatHourMinSec

// TODO 跳转历史记录
@Composable
fun BackToHistoryTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    time: String
) {
    SkipTip(
        modifier = modifier,
        show = show,
        text = "上次看到 $time 点击确认键跳转"
    )
}

@Composable
fun SkipOpTip(
    modifier: Modifier = Modifier,
    show: Boolean
) {
    SkipTip(
        modifier = modifier,
        show = show,
        text = "跳过片头"
    )
}

@Composable
fun SkipEdTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    secondsLeft: Int = 0
) {
    SkipTip(
        modifier = modifier,
        show = show,
        text = if (secondsLeft > 0) "${secondsLeft} 秒后跳过片尾" else "跳过片尾"
    )
}

@Composable
fun SkipTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    text: String
) {
    AnimatedVisibility(
        visible = show,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Surface(
                modifier = modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 32.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                shape = MaterialTheme.shapes.medium.copy(
                    topStart = CornerSize(0.dp), bottomStart = CornerSize(0.dp)
                )
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = text,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

/**
 * 播放器左下角的提示区域。
 *
 * 优先级：历史记录续播 > 跳过片头 > 跳过片尾。
 * 片头/片尾时间来自 PGC 接口，普通投稿没有该数据时不会显示。
 */
@Composable
fun SkipTips(
    modifier: Modifier = Modifier,
) {
    val videoPlayerHistoryData = LocalVideoPlayerHistoryData.current
    val videoPlayerStateData = LocalVideoPlayerStateData.current
    val videoPlayerSeekData = LocalVideoPlayerSeekData.current
    val configData = LocalVideoPlayerConfigData.current

    val skipInfo = configData.currentSkipInfo
    val positionSec = (videoPlayerSeekData.position / 1000).toInt()
    val durationSec = (videoPlayerSeekData.duration / 1000).toInt()

    // 只有在片头区间内才提示；片尾在 edStart 之后提示
    val inOp = skipInfo != null && positionSec in skipInfo.opStart until skipInfo.opEnd
    val inEd = skipInfo != null && edStartReached(positionSec, durationSec, skipInfo)

    // SponsorBlock 片段优先于 PGC 片头片尾提示，因为它更具体
    val activeSponsor = configData.sponsorSegments.firstOrNull {
        it.contains(videoPlayerSeekData.position) &&
            it.skipType != dev.aaa1115910.bv.player.entity.SponsorSegmentSkipType.ShowOnly &&
            it.skipType != dev.aaa1115910.bv.player.entity.SponsorSegmentSkipType.Disabled &&
            !(it.skipType == dev.aaa1115910.bv.player.entity.SponsorSegmentSkipType.SkipOnce && it.hasSkipped)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (videoPlayerStateData.showBackToHistory) {
            BackToHistoryTip(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 32.dp),
                show = true,
                time = videoPlayerHistoryData.lastPlayed.toLong().formatHourMinSec()
            )
        } else if (activeSponsor != null) {
            SkipTip(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 32.dp),
                show = true,
                text = "点击确认键跳过${activeSponsor.shortName}"
            )
        } else if (inOp) {
            SkipOpTip(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 32.dp),
                show = true
            )
        } else if (inEd) {
            SkipEdTip(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 32.dp),
                show = true,
                secondsLeft = (skipInfo!!.edStart - positionSec).coerceAtLeast(0)
            )
        }
    }
}

/**
 * 判断当前是否已进入片尾可跳过区间。
 * B 站返回的 edStart/edEnd 是相对正片的秒数，edEnd 可能为 0，此时以视频总时长为准。
 */
private fun edStartReached(
    positionSec: Int,
    durationSec: Int,
    skipInfo: dev.aaa1115910.bv.player.entity.SkipInfo
): Boolean {
    if (skipInfo.edStart <= 0) return false
    val end = if (skipInfo.edEnd > skipInfo.edStart) skipInfo.edEnd else durationSec
    return durationSec > 0 && positionSec in skipInfo.edStart until end
}

package dev.aaa1115910.bv.player.entity

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem

data class VideoPlayerSeekData(
    val duration: Long = 1,
    val position: Long = 0,
    val bufferedPercentage: Int = 0
)

data class VideoPlayerSeekThumbData(
    val idleIcon: String = "",
    val movingIcon: String = "",
)

data class VideoPlayerVideoInfoData(
    val width: Int = 0,
    val height: Int = 0,
    val codec: String = "",
    val title: String = "Title",
    val partTitle: String = "PartTitle",
)

data class VideoPlayerClockData(
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
)

data class VideoPlayerLogsData(
    val logs: String = "",
)

data class VideoPlayerHistoryData(
    val lastPlayed: Int = 0,
    val showBackToHistory: Boolean = false,
)

data class VideoPlayerPaymentData(
    val needPay: Boolean = false,
    val epid: Int = 0,
)

data class VideoPlayerLoadStateData(
    val loadState: RequestState = RequestState.Ready,
    val errorMessage: String = "",
)

data class VideoPlayerStateData(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isError: Boolean = false,
    val exception: Exception? = null,
    val showBackToHistory: Boolean = false,
)

data class VideoPlayerConfigData(
    val availableResolutions: List<Resolution> = emptyList(),
    val availableVideoCodec: List<VideoCodec> = emptyList(),
    val availableAudio: List<Audio> = emptyList(),
    val availableSubtitleTracks: List<Subtitle> = emptyList(),
    val availableVideoList: List<VideoListItem> = emptyList(),
    val currentVideoCid: Long = 0,
    val currentResolution: Resolution = Resolution.R240P,
    val currentVideoCodec: VideoCodec = VideoCodec.AVC,
    val currentVideoAspectRatio: VideoAspectRatio = VideoAspectRatio.Default,
    val currentVideoSpeed: Float = 1f,
    val currentAudio: Audio = Audio.A192K,
    val currentDanmakuEnabled: Boolean = true,
    val currentDanmakuEnabledList: List<DanmakuType> = listOf(),
    val currentDanmakuSize: DanmakuSize = DanmakuSize.S2,
    val currentDanmakuScale: Float = 1f,
    val currentDanmakuTransparency: DanmakuTransparency = DanmakuTransparency.T1,
    val currentDanmakuOpacity: Float = 1f,
    val currentDanmakuArea: Float = 1f,
    val currentDanmakuMask: Boolean = false,
    val currentSubtitleId: Long = 0,
    val currentSubtitleData: List<SubtitleItem> = emptyList(),
    val currentSubtitleFontSize: TextUnit = 24.sp,
    val currentSubtitleBackgroundOpacity: Float = 0.4f,
    val currentSubtitleBottomPadding: Dp = 12.dp,
    val currentPlayMode: PlayMode = PlayMode.Sequential,
    val incognitoMode: Boolean = false,
    /** 片头开始/结束、片尾开始/结束时间（秒），为 null 表示该视频不提供跳过信息 */
    val currentSkipInfo: SkipInfo? = null,
    /** SponsorBlock 片段，按开始时间升序 */
    val sponsorSegments: List<SponsorSegmentInfo> = emptyList(),
)

/**
 * 片头片尾跳过区间，单位秒
 */
data class SkipInfo(
    val opStart: Int,
    val opEnd: Int,
    val edStart: Int,
    val edEnd: Int
)

/**
 * SponsorBlock 片段，时间单位毫秒
 *
 * @param category 分类值，见 SponsorBlockCategory
 * @param categoryName 分类显示名
 */
data class SponsorSegmentInfo(
    val uuid: String,
    val category: String,
    val categoryName: String,
    val startMs: Long,
    val endMs: Long,
    val hasSkipped: Boolean = false
) {
    fun contains(positionMs: Long): Boolean = positionMs in startMs until endMs
}

data class VideoPlayerDanmakuMasksData(
    val danmakuMasks: List<DanmakuMaskSegment> = emptyList(),
)

data class VideoPlayerVideoShotData(
    val videoShot: VideoShot? = null,
)

data class VideoPlayerDebugInfoData(
    val debugInfo: String = "",
)

val LocalVideoPlayerSeekData = compositionLocalOf { VideoPlayerSeekData() }
val LocalVideoPlayerSeekThumbData = compositionLocalOf { VideoPlayerSeekThumbData() }
val LocalVideoPlayerVideoInfoData = compositionLocalOf { VideoPlayerVideoInfoData() }
val LocalVideoPlayerClockData = compositionLocalOf { VideoPlayerClockData() }
val LocalVideoPlayerLogsData = compositionLocalOf { VideoPlayerLogsData() }
val LocalVideoPlayerHistoryData = compositionLocalOf { VideoPlayerHistoryData() }
val LocalVideoPlayerPaymentData = compositionLocalOf { VideoPlayerPaymentData() }
val LocalVideoPlayerLoadStateData = compositionLocalOf { VideoPlayerLoadStateData() }
val LocalVideoPlayerStateData = compositionLocalOf { VideoPlayerStateData() }
val LocalVideoPlayerConfigData = compositionLocalOf { VideoPlayerConfigData() }
val LocalVideoPlayerDanmakuMasksData = compositionLocalOf { VideoPlayerDanmakuMasksData() }
val LocalVideoPlayerVideoShotData = compositionLocalOf { VideoPlayerVideoShotData() }
val LocalVideoPlayerDebugInfoData = compositionLocalOf { VideoPlayerDebugInfoData() }
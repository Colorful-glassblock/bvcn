package dev.aaa1115910.bv.mobile.screen.settings.details

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.schnettler.datastore.manager.DataStoreManager
import dev.aaa1115910.bv.dataStore
import dev.aaa1115910.bv.mobile.component.preferences.RadioPreferenceItem
import dev.aaa1115910.bv.mobile.component.preferences.SwitchPreferenceItem
import dev.aaa1115910.bv.mobile.component.preferences.TextPreferenceItem
import dev.aaa1115910.bv.mobile.component.preferences.getOrDefault
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.util.PrefKeys
import dev.aaa1115910.bv.util.Prefs

@Composable
fun PlayContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataStoreManager = DataStoreManager(LocalContext.current.dataStore)
    val prefs by dataStoreManager.preferenceFlow.collectAsState(initial = null)

    var sponsorBlockSettings by remember { mutableStateOf(Prefs.sponsorBlockSettings) }
    var showSponsorBlockSettings by remember { mutableStateOf(false) }

    if (showSponsorBlockSettings) {
        SponsorBlockSettingsDialog(
            show = showSponsorBlockSettings,
            settings = sponsorBlockSettings,
            onDismiss = { showSponsorBlockSettings = false },
            onSelect = { category, type ->
                Prefs.setSponsorBlockSkipType(category, type)
                sponsorBlockSettings = Prefs.sponsorBlockSettings
            }
        )
    }

    Column(
        modifier = modifier
    ) {
        RadioPreferenceItem(
            title = "默认画质",
            items = Resolution.entries.associate { it.code to it.getDisplayName(context) },
            prefReq = PrefKeys.prefDefaultQualityRequest,
            summary = Resolution.fromCode(prefs.getOrDefault(PrefKeys.prefDefaultQualityRequest))
                ?.getDisplayName(context) ?: ""
        )
        RadioPreferenceItem(
            title = "默认视频编码",
            items = listOf(VideoCodec.AVC, VideoCodec.HEVC, VideoCodec.AV1)
                .associate { it.codecId to it.getDisplayName(context) },
            prefReq = PrefKeys.prefDefaultVideoCodecRequest,
            summary = VideoCodec.fromCode(prefs.getOrDefault(PrefKeys.prefDefaultVideoCodecRequest))
                ?.getDisplayName(context) ?: ""
        )
        RadioPreferenceItem(
            title = "默认音频",
            items = Audio.entries.associate { it.code to it.getDisplayName(context) },
            prefReq = PrefKeys.prefDefaultAudioRequest,
            summary = Audio.fromCode(prefs.getOrDefault(PrefKeys.prefDefaultAudioRequest))
                ?.getDisplayName(context) ?: ""
        )
        SwitchPreferenceItem(
            title = "启用视频软解",
            summary = "使用软件解码器播放视频，可能解决硬解兼容性问题",
            prefReq = PrefKeys.prefEnableSoftwareVideoRendererRequest
        )
        SwitchPreferenceItem(
            title = "启用赞助片段拦截",
            summary = "通过 BilibiliSponsorBlock 社区数据标记并跳过赞助片段",
            prefReq = PrefKeys.prefEnableSponsorBlockRequest
        )
        SwitchPreferenceItem(
            title = "发评反诈",
            summary = "发送评论后检查评论是否只有自己可见（shadow ban）",
            prefReq = PrefKeys.prefEnableCommentAntifraudRequest
        )
        TextPreferenceItem(
            title = "赞助片段分类设置",
            summary = "为每个片段分类选择跳过方式",
            onClick = { showSponsorBlockSettings = true }
        )
    }
}

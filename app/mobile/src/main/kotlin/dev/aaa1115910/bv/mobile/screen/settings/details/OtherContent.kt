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
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.dataStore
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.mobile.component.preferences.RadioPreferenceItem
import dev.aaa1115910.bv.mobile.component.preferences.SwitchPreferenceItem
import dev.aaa1115910.bv.mobile.component.preferences.TextPreferenceItem
import dev.aaa1115910.bv.mobile.component.preferences.getOrDefault
import dev.aaa1115910.bv.util.PrefKeys
import dev.aaa1115910.bv.util.Prefs

@Composable
fun OtherContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataStoreManager = DataStoreManager(LocalContext.current.dataStore)
    val prefs by dataStoreManager.preferenceFlow.collectAsState(initial = null)

    var showFps by remember { mutableStateOf(Prefs.showFps) }
    var updateAlpha by remember { mutableStateOf(Prefs.updateAlpha) }

    Column(modifier = modifier) {
        RadioPreferenceItem(
            title = "主题",
            items = ThemeType.entries.associate { it.ordinal to it.getDisplayName(context) },
            prefReq = PrefKeys.prefThemeTypeRequest,
            summary = ThemeType.entries[prefs.getOrDefault(PrefKeys.prefThemeTypeRequest)]
                .getDisplayName(context)
        )
        SwitchPreferenceItem(
            title = "显示 FPS",
            summary = "在屏幕左上角显示 FPS，需要重启 App",
            checked = showFps,
            onCheckedChange = {
                showFps = it
                Prefs.showFps = it
            }
        )
        SwitchPreferenceItem(
            title = "启用音频软解",
            summary = "尝试使用 FFmpeg 播放硬解不兼容的音频",
            prefReq = PrefKeys.prefEnableFfmpegEndererRequest
        )
        SwitchPreferenceItem(
            title = "Alpha 版更新",
            summary = "接收预览版本更新",
            checked = updateAlpha,
            onCheckedChange = {
                updateAlpha = it
                Prefs.updateAlpha = it
            }
        )
        TextPreferenceItem(
            title = "版本",
            summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
    }
}

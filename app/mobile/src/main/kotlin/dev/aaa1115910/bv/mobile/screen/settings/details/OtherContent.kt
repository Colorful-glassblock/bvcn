package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.mobile.component.preferences.items.listItemPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.util.FirebaseUtil
import dev.aaa1115910.bv.util.PrefKeys
import dev.aaa1115910.bv.util.Prefs

@Composable
fun OtherContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showFps by remember { mutableStateOf(Prefs.showFps) }
    var updateAlpha by remember { mutableStateOf(Prefs.updateAlpha) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
            "界面" to {
                radioPreference(
                    title = "主题",
                    prefReq = PrefKeys.prefThemeTypeRequest,
                    values = ThemeType.entries
                        .associate { it.ordinal to it.getDisplayName(context) }
                        .toSortedMap { a, b -> a.compareTo(b) }
                )
                switchPreference(
                    title = "显示 FPS",
                    summary = "在屏幕左上角显示 FPS，需要重启 App",
                    checked = showFps,
                    onCheckedChange = {
                        showFps = it
                        Prefs.showFps = it
                    }
                )
            },
            "播放器" to {
                switchPreference(
                    title = "启用音频软解",
                    summary = "尝试使用 FFmpeg 播放硬解不兼容的音频",
                    prefReq = PrefKeys.prefEnableFfmpegEndererRequest,
                    onCheckedChange = { true }
                )
            },
            "诊断与更新" to {
                switchPreference(
                    title = "发送错误日志",
                    summary = "这将帮助开发者更快地发现与解决大部分问题",
                    checked = Prefs.enableFirebaseCollection,
                    onCheckedChange = {
                        Prefs.enableFirebaseCollection = it
                        FirebaseUtil.setCrashlyticsCollectionEnabled(it)
                    }
                )
                switchPreference(
                    title = "Alpha 版更新",
                    summary = "接收预览版本更新",
                    checked = updateAlpha,
                    onCheckedChange = {
                        updateAlpha = it
                        Prefs.updateAlpha = it
                    }
                )
            },
            "关于" to {
                listItemPreference(
                    title = "版本",
                    summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                )
            }
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OtherContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            OtherContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

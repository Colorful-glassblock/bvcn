package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorBlockCategory
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSkipType
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.settings.SettingListItem

/**
 * SponsorBlock 分类设置：先选分类，再选该分类的处理方式。
 */
@Composable
fun SponsorBlockSettingsDialog(
    show: Boolean,
    settings: List<SponsorSkipType>,
    onDismiss: () -> Unit,
    onSelect: (SponsorBlockCategory, SponsorSkipType) -> Unit
) {
    if (!show) return

    var selectedCategory by remember { mutableStateOf<SponsorBlockCategory?>(null) }

    if (selectedCategory == null) {
        TvAlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = "关闭"
                )
            },
            title = { Text(text = "赞助片段分类") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(SponsorBlockCategory.entries) { category ->
                        val current = settings.getOrNull(category.ordinal)
                            ?: category.defaultSkipType
                        SettingListItem(
                            title = category.displayName,
                            supportText = current.displayName,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }
        )
    } else {
        val category = selectedCategory!!
        TvAlertDialog(
            onDismissRequest = { selectedCategory = null },
            confirmButton = {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = "返回",
                    style = MaterialTheme.typography.labelLarge
                )
            },
            title = { Text(text = category.displayName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SponsorSkipType.entries.forEach { type ->
                        SettingListItem(
                            title = type.displayName,
                            supportText = "",
                            onClick = {
                                onSelect(category, type)
                                selectedCategory = null
                            }
                        )
                    }
                }
            }
        )
    }
}

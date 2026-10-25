package dev.aaa1115910.bv.mobile.screen.settings.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorBlockCategory
import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSkipType

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
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(text = "关闭") }
            },
            title = { Text(text = "赞助片段分类") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(SponsorBlockCategory.entries) { category ->
                        val current = settings.getOrNull(category.ordinal)
                            ?: category.defaultSkipType
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = category }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = current.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        )
    } else {
        val category = selectedCategory!!
        AlertDialog(
            onDismissRequest = { selectedCategory = null },
            confirmButton = {
                TextButton(onClick = { selectedCategory = null }) { Text(text = "返回") }
            },
            title = { Text(text = category.displayName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SponsorSkipType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(category, type)
                                    selectedCategory = null
                                }
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = type.displayName)
                        }
                    }
                }
            }
        )
    }
}

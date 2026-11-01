package dev.aaa1115910.bv.mobile.component.reply

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.reply.Comment

/**
 * 发评论输入对话框。
 *
 * @param replyTo 非空时表示回复这条评论，否则发顶层评论
 */
@Composable
fun SendCommentDialog(
    replyTo: Comment? = null,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (replyTo == null) {
                    "发表评论"
                } else {
                    "回复 @${replyTo.member.name}"
                }
            )
        },
        text = {
            Column {
                if (replyTo != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = replyTo.content.joinToString(""),
                        maxLines = 3
                    )
                }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(text = if (replyTo == null) "说点什么…" else "回复 @${replyTo.member.name}…")
                    },
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onSend(text.trim()) }
            ) {
                Text(text = "发送")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "取消") }
        }
    )
}

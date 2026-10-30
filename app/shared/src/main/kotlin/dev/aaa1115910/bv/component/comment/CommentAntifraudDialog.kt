package dev.aaa1115910.bv.component.comment

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import dev.aaa1115910.bv.util.CommentAntifraud

/**
 * 发评反诈结果弹窗。移动端和 TV 端共用。
 */
@Composable
fun CommentAntifraudDialog(
    result: CommentAntifraud.CheckResult?,
    onDismiss: () -> Unit
) {
    if (result == null) return

    val isBad = result.result == CommentAntifraud.Result.ShadowBanned
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (result.result) {
                    CommentAntifraud.Result.Visible -> "评论检查结果：正常"
                    CommentAntifraud.Result.ShadowBanned -> "评论检查结果：疑似被屏蔽"
                    CommentAntifraud.Result.NotFound -> "评论检查结果：暂不可见"
                },
                color = if (isBad) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        },
        text = { Text(text = result.message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "知道了") }
        }
    )
}

package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import dev.aaa1115910.biliapi.entity.reply.Comment

/**
 * 播放器评论面板的数据。
 *
 * 放在 player 模块而不是 bili-api，避免播放器依赖评论仓库。
 */
@Stable
class CommentPanelState {
    val comments = mutableStateListOf<Comment>()
    var loading by mutableStateOf(false)
    var hasMore by mutableStateOf(true)
    var loaded by mutableStateOf(false)

    fun reset() {
        comments.clear()
        loading = false
        hasMore = true
        loaded = false
    }
}

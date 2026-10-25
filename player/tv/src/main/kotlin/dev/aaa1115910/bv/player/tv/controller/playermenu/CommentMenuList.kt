package dev.aaa1115910.bv.player.tv.controller.playermenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.tv.controller.CommentPanelState
import dev.aaa1115910.bv.player.tv.controller.LocalMenuFocusStateData
import dev.aaa1115910.bv.player.tv.controller.MenuFocusState

/**
 * 播放器菜单里的评论页：上半部分是发评论输入框，下半部分是评论列表。
 *
 * 电视上没有软键盘，输入框只负责"弹起输入状态"，实际文字由系统输入法处理。
 */
@Composable
fun CommentMenuList(
    modifier: Modifier = Modifier,
    panelState: CommentPanelState,
    onFocusStateChange: (MenuFocusState) -> Unit,
    onSendComment: (String) -> Unit,
    onLoadComments: (suspend () -> Unit)? = null
) {
    val focusState = LocalMenuFocusStateData.current
    val listState = rememberLazyListState()
    val sendFocusRequester = remember { FocusRequester() }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sendHadFocus by remember { mutableStateOf(false) }

    // 首次进入评论页时拉起第一页
    LaunchedEffect(Unit) {
        if (!panelState.loaded && onLoadComments != null) {
            onLoadComments()
        }
    }

    // 滚动到底部继续加载
    LaunchedEffect(listState, panelState.comments.size, panelState.loading) {
        if (panelState.loading || !panelState.hasMore || onLoadComments == null) return@LaunchedEffect
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= panelState.comments.size - 3) onLoadComments()
    }

    Column(
        modifier = modifier
            .width(420.dp)
            .fillMaxHeight()
            .padding(horizontal = 16.dp)
    ) {
        // 输入框
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(sendFocusRequester)
                .onFocusChanged { sendHadFocus = it.hasFocus }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                    ) {
                        if (draft.isNotBlank() && !sending) {
                            sending = true
                            onSendComment(draft)
                            draft = ""
                            sending = false
                        }
                        true
                    } else {
                        false
                    }
                }
                .padding(vertical = 12.dp),
            value = draft,
            onValueChange = { draft = it },
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Column {
                    Text(
                        text = if (draft.isEmpty()) "说点什么…（确认键发送）" else "",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    innerTextField()
                }
            }
        )

        if (panelState.loading && panelState.comments.isEmpty()) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = "加载中…",
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(items = panelState.comments) { comment ->
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    headlineContent = {
                        Text(
                            text = comment.member.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF7FD4FF)
                        )
                    },
                    supportingContent = {
                        Column {
                            Text(
                                text = comment.content.joinToString(""),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = comment.timeDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                )
            }
            if (panelState.comments.isEmpty() && !panelState.loading) {
                item {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "暂无评论",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (panelState.hasMore) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                text = "继续下滑加载更多",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

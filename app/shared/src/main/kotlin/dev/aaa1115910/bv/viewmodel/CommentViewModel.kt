package dev.aaa1115910.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.CommentAntifraud
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class CommentViewModel(
    private val commentRepository: CommentRepository
) : ViewModel() {
    companion object {
        val logger = KotlinLogging.logger {}

        /** 评论发出后需要时间进入未登录视角，等待 8 秒再检查是否被 shadow ban */
        private const val ANTIFRAUD_CHECK_DELAY_MS = 8_000L
    }

    var commentId = 0L
    var commentType = 0L

    val comments = mutableStateListOf<Comment>()
    val replies = mutableStateListOf<Comment>()
    var replyRootComment by mutableStateOf<Comment?>(null)

    var rpid by mutableLongStateOf(0L)
    var rpCount by mutableIntStateOf(0)

    var nextCommentPage = CommentPage()
    var nextCommentReplyPage = CommentReplyPage()

    var commentSort by mutableStateOf(CommentSort.Hot)
    var replySort by mutableStateOf(CommentSort.Time)

    var hasMoreComments by mutableStateOf(true)
    var hasMoreReplies by mutableStateOf(true)
    var refreshingComments by mutableStateOf(false)
    var refreshingReplies by mutableStateOf(false)
    var updatingComments by mutableStateOf(false)
    var updatingReplies by mutableStateOf(false)

    /** 视频/动态作者 mid，作者可以删除自己评论区下的任意评论 */
    var ownerMid by mutableStateOf<Long?>(null)

    val currentUserMid: Long?
        get() = commentRepository.currentUserMid

    fun canDelete(comment: Comment): Boolean {
        val currentMid = commentRepository.currentUserMid ?: return false
        return comment.mid == currentMid || comment.mid == ownerMid
    }

    suspend fun loadMoreComment() {
        if (updatingComments) return
        updatingComments = true
        if (!hasMoreComments) {
            updatingComments = false
            delay(300)
            refreshingComments = false
            return
        }
        logger.fInfo { "Load more comments: [commentId=$commentId, commentType=$commentType, page=$nextCommentPage]" }
        runCatching {
            val commentsData = commentRepository.getComments(
                id = commentId,
                type = commentType,
                page = nextCommentPage,
                sort = commentSort,
                preferApiType = Prefs.apiType
            )
            nextCommentPage = commentsData.nextPage
            hasMoreComments = commentsData.hasNext
            comments.addAll(commentsData.comments)
        }.onFailure {
            logger.fException(it) { "Load more comments failed" }
            withContext(Dispatchers.Main) {
                "加载评论失败：${it.localizedMessage}".toast(BVApp.context)
            }
        }
        updatingComments = false
        delay(300)
        refreshingComments = false
    }

    suspend fun switchCommentSort(newSort: CommentSort) {
        logger.fInfo { "Switch comment sort to ${newSort.name}" }
        commentSort = newSort
        refreshComments()
    }

    suspend fun refreshComments() {
        refreshingComments = true
        logger.fInfo { "refresh comments" }
        nextCommentPage = CommentPage()
        hasMoreComments = true
        comments.clear()
        loadMoreComment()
    }

    suspend fun loadMoreReplies() {
        if (updatingReplies) return
        updatingReplies = true
        if (!hasMoreReplies) {
            updatingReplies = false
            delay(300)
            refreshingReplies = false
            return
        }
        logger.fInfo { "Load more replies, commentId=$commentId, commentType=$commentType, page=$nextCommentReplyPage" }
        runCatching {
            val commentRepliesData = commentRepository.getCommentReplies(
                rpid = rpid,
                type = commentType,
                commentId = commentId,
                page = nextCommentReplyPage,
                sort = replySort,
                preferApiType = Prefs.apiType
            )
            nextCommentReplyPage = commentRepliesData.nextPage
            hasMoreReplies = commentRepliesData.hasNext
            if (replyRootComment == null) replyRootComment = commentRepliesData.rootComment
            replies.addAll(commentRepliesData.replies)
        }.onFailure {
            logger.fException(it) { "Load more replies failed" }
        }
        updatingReplies = false
        delay(300)
        refreshingReplies = false
    }

    suspend fun switchReplySort(newSort: CommentSort) {
        logger.fInfo { "Switch reply sort to ${newSort.name}" }
        replySort = newSort
        refreshReplies()
    }

    suspend fun refreshReplies() {
        refreshingReplies = true
        logger.fInfo { "refresh replies" }
        nextCommentReplyPage = CommentReplyPage()
        hasMoreReplies = true
        replies.clear()
        loadMoreReplies()
    }

    /** 评论发送状态 */
    var sendingComment by mutableStateOf(false)

    /** 评论删除状态，避免重复提交 */
    var deletingComment by mutableStateOf(false)

    /** 最近一次反诈检查结果，UI 弹窗消费后置空 */
    var antifraudResult by mutableStateOf<CommentAntifraud.CheckResult?>(null)

    /**
     * 回复指定评论。
     *
     * 根评论的 root/parent 都是自身 rpid；二级评论的 root 是所属根评论，
     * parent 是被回复的这条二级评论。
     */
    suspend fun sendComment(message: String, replyTo: Comment?): Boolean {
        if (replyTo == null) return sendComment(message)
        val root = replyTo.root.takeIf { it != 0L } ?: replyTo.rpid
        return sendComment(
            message = message,
            root = root,
            parent = replyTo.rpid
        )
    }

    /**
     * 发表评论，可选在发送后做 shadow ban 检查。
     *
     * @param message 评论内容
     * @param root 根评论 id，回复二级评论时填写
     * @param parent 父评论 id，回复二级评论时填写
     */
    suspend fun sendComment(
        message: String,
        root: Long = 0,
        parent: Long = 0
    ): Boolean {
        if (sendingComment) return false
        if (message.isBlank()) return false
        sendingComment = true
        var success = false
        runCatching {
            val newRpid = commentRepository.postComment(
                oid = commentId,
                type = commentType,
                message = message,
                root = root,
                parent = parent
            )
            logger.fInfo { "Comment posted, rpid=$newRpid" }
            success = true

            // 先刷新列表，让新评论尽快出现
            if (root == 0L || rpid != root) {
                refreshComments()
            } else {
                refreshReplies()
            }

            if (Prefs.enableCommentAntifraud && newRpid != 0L) {
                withContext(Dispatchers.Main) {
                    "评论已发送，8 秒后检查是否被屏蔽".toast(BVApp.context)
                }
                // 评论索引需要几秒才对外可见，8 秒后再对比登录/未登录结果。
                // 用 viewModelScope 独立执行，不阻塞发送流程。
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        delay(ANTIFRAUD_CHECK_DELAY_MS)
                        val result = CommentAntifraud.check(
                            commentRepository = commentRepository,
                            oid = commentId,
                            type = commentType,
                            rpid = newRpid,
                            root = root,
                            message = message
                        )
                        withContext(Dispatchers.Main) { antifraudResult = result }
                    }.onFailure {
                        logger.fException(it) { "Antifraud check failed" }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    "评论已发送".toast(BVApp.context)
                }
            }
        }.onFailure {
            logger.fException(it) { "Send comment failed" }
            withContext(Dispatchers.Main) {
                "发送失败：${it.localizedMessage}".toast(BVApp.context)
            }
        }
        sendingComment = false
        return success
    }

    /**
     * 删除评论。删除后从本地列表移除并重新拉取评论列表，更新内嵌回复和计数。
     */
    suspend fun deleteComment(comment: Comment): Boolean {
        if (deletingComment) return false
        deletingComment = true
        var success = false
        runCatching {
            commentRepository.deleteComment(
                oid = comment.oid,
                type = comment.type,
                rpid = comment.rpid
            )
            logger.fInfo { "Comment deleted, rpid=${comment.rpid}" }
            success = true

            withContext(Dispatchers.Main) {
                comments.removeAll { it.rpid == comment.rpid }
                replies.removeAll { it.rpid == comment.rpid }
                if (replyRootComment?.rpid == comment.rpid || rpid == comment.rpid) {
                    replyRootComment = null
                    replies.clear()
                }
                "评论已删除".toast(BVApp.context)
            }

            // 根评论里内嵌的回复预览和回复数需要服务端重新计算
            if (rpid != comment.rpid) {
                runCatching { refreshComments() }
            }
        }.onFailure {
            logger.fException(it) { "Delete comment failed" }
            withContext(Dispatchers.Main) {
                "删除失败：${it.localizedMessage}".toast(BVApp.context)
            }
        }
        deletingComment = false
        return success
    }
}

package dev.aaa1115910.biliapi.repositories

import bilibili.main.community.reply.v1.Mode
import bilibili.main.community.reply.v1.ReplyGrpcKt
import bilibili.main.community.reply.v1.detailListReq
import bilibili.main.community.reply.v1.mainListReq
import bilibili.pagination.feedPagination
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentRepliesData
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.entity.reply.CommentsData
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class CommentRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val replyStub
        get() = runCatching {
            ReplyGrpcKt.ReplyCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getComments(
        id: Long,
        type: Long,
        sort: CommentSort = CommentSort.Hot,
        page: CommentPage = CommentPage(),
        preferApiType: ApiType = ApiType.Web
    ): CommentsData {
        when (preferApiType) {
            ApiType.Web -> {
                val webComments = BiliHttpApi.getComments(
                    oid = id,
                    type = type,
                    mode = sort.param,
                    paginationStr = Json.encodeToString(mapOf("offset" to page.nextWebPage)),
                    sessData = authRepository.sessionData ?: "",
                    buvid3 = authRepository.buvid3 ?: ""
                ).getResponseData()
                return CommentsData.fromCommentData(webComments)
            }

            ApiType.App -> {
                runCatching {
                    val appComments = replyStub?.mainList(
                        mainListReq {
                            this.oid = id.toLong()
                            this.type = type.toLong()
                            mode = when (sort) {
                                CommentSort.Hot -> Mode.MAIN_LIST_HOT
                                CommentSort.HotAndTime -> Mode.DEFAULT
                                CommentSort.Time -> Mode.MAIN_LIST_TIME
                            }
                            pagination = feedPagination {
                                offset = page.nextAppPage
                            }
                        }
                    ) ?: throw IllegalStateException("Reply stub is not initialized")
                    return CommentsData.fromMainListReply(appComments)
                }.onFailure {
                    handleGrpcException(it)
                }.getOrThrow()
            }
        }
    }

    suspend fun getCommentReplies(
        rpid: Long,
        type: Long,
        commentId: Long,
        page: CommentReplyPage = CommentReplyPage(),
        sort: CommentSort = CommentSort.Hot,
        preferApiType: ApiType = ApiType.Web
    ): CommentRepliesData {
        when (preferApiType) {
            ApiType.Web -> {
                val webReplies = BiliHttpApi.getCommentReplies(
                    oid = commentId,
                    type = type,
                    root = rpid,
                    pageSize = 20,
                    pageNumber = page.nextWebPage,
                ).getResponseData()
                return CommentRepliesData.fromCommentReplyData(webReplies)
            }

            ApiType.App -> {
                val appReplies = replyStub?.detailList(
                    detailListReq {
                        this.oid = commentId
                        this.type = type
                        root = rpid
                        mode = when (sort) {
                            CommentSort.Hot -> Mode.MAIN_LIST_HOT
                            CommentSort.HotAndTime -> Mode.DEFAULT
                            CommentSort.Time -> Mode.MAIN_LIST_TIME
                        }
                        pagination = feedPagination {
                            offset = page.nextAppPage
                        }
                    }
                ) ?: throw IllegalStateException("Reply stub is not initialized")
                return CommentRepliesData.fromCommentReplyList(appReplies)
            }
        }
    }

    /**
     * 发表评论。仅支持 Web 接口。
     *
     * @param oid 评论区 id，视频为 avid
     * @param type 评论区类型，视频为 1
     * @param message 评论内容
     * @param root 根评论 id，回复二级评论时填写
     * @param parent 父评论 id，回复二级评论时填写
     * @throws IllegalStateException 未登录或接口返回错误
     */
    suspend fun postComment(
        oid: Long,
        type: Long = 1,
        message: String,
        root: Long = 0,
        parent: Long = 0
    ): Long {
        val csrf = authRepository.biliJct ?: throw IllegalStateException("未登录")
        val sessData = authRepository.sessionData ?: throw IllegalStateException("未登录")
        val response = BiliHttpApi.postComment(
            type = type,
            oid = oid,
            message = message,
            root = root,
            parent = parent,
            csrf = csrf,
            sessData = sessData
        )
        return response.getResponseData().rpid
    }

    /**
     * 未登录状态下翻找根评论下的二级评论，判断指定 rpid 是否可见。
     *
     * 返回 null 表示翻页结束仍未找到；抛异常表示未登录请求被拒绝。
     */
    suspend fun findReplyWithoutLogin(
        oid: Long,
        type: Long,
        root: Long,
        rpid: Long,
        maxPages: Int = 10
    ): Boolean {
        for (page in 1..maxPages) {
            val data = runCatching {
                BiliHttpApi.getCommentRepliesForCheck(
                    oid = oid,
                    type = type,
                    root = root,
                    pageNumber = page,
                    sessData = null
                ).getResponseData()
            }.getOrElse { return false }

            if (data.replies.isEmpty()) return false
            if (data.replies.any { it.rpid == rpid }) return true
        }
        return false
    }

    /**
     * 登录状态下翻找，作为对照。
     */
    suspend fun findReplyWithLogin(
        oid: Long,
        type: Long,
        root: Long,
        rpid: Long,
        maxPages: Int = 10
    ): Boolean {
        val sessData = authRepository.sessionData ?: return false
        val csrf = authRepository.biliJct
        for (page in 1..maxPages) {
            val data = runCatching {
                BiliHttpApi.getCommentRepliesForCheck(
                    oid = oid,
                    type = type,
                    root = root,
                    pageNumber = page,
                    sessData = sessData,
                    csrf = csrf
                ).getResponseData()
            }.getOrElse { return false }

            if (data.replies.isEmpty()) return false
            if (data.replies.any { it.rpid == rpid }) return true
        }
        return false
    }
}

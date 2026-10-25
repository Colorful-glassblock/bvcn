package dev.aaa1115910.biliapi.http.entity.reply

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 发表评论的返回数据。
 *
 * 接口正常返回时只给出新评论的 rpid 等信息，评论实体本身不在此处，
 * 需要调用方自行拉取评论列表确认是否可见。
 */
@Serializable
data class CommentAddData(
    @SerialName("rpid")
    val rpid: Long = 0,
    @SerialName("rpid_str")
    val rpidStr: String = "",
    @SerialName("dialog")
    val dialog: Long = 0,
    @SerialName("dialog_str")
    val dialogStr: String = "",
    @SerialName("root")
    val root: Long = 0,
    @SerialName("root_str")
    val rootStr: String = "",
    @SerialName("parent")
    val parent: Long = 0,
    @SerialName("parent_str")
    val parentStr: String = "",
    @SerialName("reply")
    val reply: CommentData.Reply? = null
)

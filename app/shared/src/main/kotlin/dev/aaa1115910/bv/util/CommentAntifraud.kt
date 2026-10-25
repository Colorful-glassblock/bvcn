package dev.aaa1115910.bv.util

import dev.aaa1115910.biliapi.repositories.CommentRepository
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 发评反诈：判断刚发出的评论是否只有自己可见（shadow ban）。
 *
 * 原理：B 站的 shadow ban 会让评论在未登录访问时不可见，但登录状态下仍能看到。
 * 所以发完评论后用**未登录**身份去翻阅，再和登录状态对照：
 *
 * - 未登录可见 → 评论正常
 * - 未登录不可见、登录可见 → 被 shadow ban
 * - 两边都不可见 → 评论被删除或仍在审核
 *
 * 参考项目：https://github.com/freedom-introvert/biliSendCommAntifraud
 */
object CommentAntifraud {
    private val logger = KotlinLogging.logger("CommentAntifraud")

    enum class Result {
        /** 评论正常，任何人可见 */
        Visible,

        /** 仅自己可见，即 shadow ban */
        ShadowBanned,

        /** 两边都找不到，可能被删除或审核中 */
        NotFound
    }

    data class CheckResult(
        val result: Result,
        val message: String
    )

    /**
     * 检查一条评论的可见性。
     *
     * @param root 根评论 id；为 0 表示检查的是主楼评论，此时无法用未登录方式精确命中，
     * 只能退化为"登录态是否可见"
     */
    suspend fun check(
        commentRepository: CommentRepository,
        oid: Long,
        type: Long,
        rpid: Long,
        root: Long,
        message: String
    ): CheckResult {
        // 主楼评论：未登录翻列表成本高且不可靠，只做登录态校验
        if (root == 0L) {
            val visibleWithLogin = commentRepository.findReplyWithLogin(
                oid = oid,
                type = type,
                root = rpid,
                rpid = rpid
            )
            val text = if (visibleWithLogin) {
                "评论已发出，登录状态下可见。\n\n你的评论：$message"
            } else {
                "评论已发出，但暂时查不到，可能正在审核。\n\n你的评论：$message"
            }
            logger.info { "Antifraud root check: visible=$visibleWithLogin" }
            return CheckResult(
                if (visibleWithLogin) Result.Visible else Result.NotFound,
                text
            )
        }

        val visibleWithoutLogin = commentRepository.findReplyWithoutLogin(
            oid = oid,
            type = type,
            root = root,
            rpid = rpid
        )
        if (visibleWithoutLogin) {
            logger.info { "Antifraud: reply $rpid visible without login" }
            return CheckResult(
                Result.Visible,
                "评论正常，任何人可见。\n\n你的评论：$message"
            )
        }

        val visibleWithLogin = commentRepository.findReplyWithLogin(
            oid = oid,
            type = type,
            root = root,
            rpid = rpid
        )
        logger.info {
            "Antifraud: reply $rpid withoutLogin=false withLogin=$visibleWithLogin"
        }
        return if (visibleWithLogin) {
            CheckResult(
                Result.ShadowBanned,
                "你的评论疑似被 shadow ban（仅自己可见）。\n\n" +
                    "可以在 B 站 App 的评论区申诉入口提交申诉。\n\n你的评论：$message"
            )
        } else {
            CheckResult(
                Result.NotFound,
                "评论已经发出，但当前查不到，可能正在审核或已被删除。\n\n你的评论：$message"
            )
        }
    }
}

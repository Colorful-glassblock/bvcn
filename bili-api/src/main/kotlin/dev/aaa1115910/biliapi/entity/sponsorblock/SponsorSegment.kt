package dev.aaa1115910.biliapi.entity.sponsorblock

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * BilibiliSponsorBlock 的片段数据
 *
 * @param uuid 片段 ID，用于上报"已看过"
 * @param category 分类，见 [SponsorBlockCategory]
 * @param actionType 建议动作
 * @param segment 起止时间，单位毫秒，下标 0 为开始、1 为结束
 * @param videoDuration 视频总时长，单位毫秒，可能为空
 */
@Serializable
data class SponsorSegment(
    @SerialName("UUID")
    val uuid: String = "",
    val category: String = "",
    val actionType: String? = null,
    val segment: List<Double> = emptyList(),
    val videoDuration: Double? = null,
    val votes: Int? = null,
) {
    /** 开始时间（毫秒） */
    val startMs: Long
        get() = ((segment.getOrNull(0) ?: 0.0) * 1000).toLong()

    /** 结束时间（毫秒） */
    val endMs: Long
        get() = ((segment.getOrNull(1) ?: 0.0) * 1000).toLong()

    /** 时长（毫秒） */
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}

/**
 * SponsorBlock 分类，取值与 BilibiliSponsorBlock 一致
 */
enum class SponsorBlockCategory(val value: String, val displayName: String) {
    Sponsor("sponsor", "赞助/恰饭"),
    SelfPromo("selfpromo", "无偿/自我推广"),
    ExclusiveAccess("exclusive_access", "独家访问"),
    Interaction("interaction", "三连/互动提醒"),
    PoiHighlight("poi_highlight", "精彩时刻"),
    Intro("intro", "过场/开场动画"),
    Outro("outro", "鸣谢/结束画面"),
    Preview("preview", "回顾/概要"),
    Padding("padding", "填充内容"),
    Filler("filler", "离题闲聊"),
    MusicOffTopic("music_offtopic", "音乐:非音乐部分"),
    Jumpscare("jumpscare", "惊吓片段");

    companion object {
        fun fromValue(value: String): SponsorBlockCategory? =
            entries.firstOrNull { it.value == value }
    }
}

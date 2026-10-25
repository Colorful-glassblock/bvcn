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
 * 每个分类的处理方式
 */
enum class SponsorSkipType(val displayName: String) {
    AlwaysSkip("总是跳过"),
    SkipOnce("跳过一次"),
    Manual("手动跳过"),
    ShowOnly("仅显示"),
    Disabled("禁用");

    companion object {
        fun fromOrdinal(ordinal: Int): SponsorSkipType =
            entries.getOrElse(ordinal) { SkipOnce }
    }
}

/**
 * SponsorBlock 分类。
 *
 * 分类标识符与颜色取自 BilibiliSponsorBlock 的公开 API 文档
 * （https://github.com/hanydd/BilibiliSponsorBlock/wiki/API）与规范，
 * 属于跨实现互操作所需的协议常量，不是从 GPL 项目复制的代码。
 */
enum class SponsorBlockCategory(
    val value: String,
    val displayName: String,
    val shortName: String,
    /** 颜色，用于进度条标记 */
    val color: Long,
    val defaultSkipType: SponsorSkipType
) {
    Sponsor("sponsor", "赞助/恰饭", "赞助", 0xFF00D400, SponsorSkipType.SkipOnce),
    SelfPromo("selfpromo", "无偿/自我推广", "推广", 0xFFFFFF00, SponsorSkipType.Manual),
    ExclusiveAccess("exclusive_access", "独家访问/抢先体验", "品牌合作", 0xFF008A5C, SponsorSkipType.ShowOnly),
    Interaction("interaction", "三连/互动提醒", "三连提醒", 0xFFCC00FF, SponsorSkipType.Manual),
    PoiHighlight("poi_highlight", "精彩时刻/重点", "精彩时刻", 0xFFFF1684, SponsorSkipType.ShowOnly),
    Intro("intro", "过场/开场动画", "开场动画", 0xFF00FFFF, SponsorSkipType.SkipOnce),
    Outro("outro", "鸣谢/结束画面", "片尾", 0xFF0202ED, SponsorSkipType.SkipOnce),
    Preview("preview", "回顾/概要", "预览", 0xFF008FD6, SponsorSkipType.Manual),
    Padding("padding", "填充内容/前黑/后黑", "填充内容", 0xFF222222, SponsorSkipType.SkipOnce),
    Filler("filler", "离题闲聊/玩笑", "离题", 0xFF7300FF, SponsorSkipType.Disabled),
    MusicOffTopic("music_offtopic", "音乐:非音乐部分", "非音乐", 0xFFFF9900, SponsorSkipType.Disabled),
    Jumpscare("jumpscare", "惊吓片段", "惊吓", 0xFFE53935, SponsorSkipType.Disabled);

    companion object {
        fun fromValue(value: String): SponsorBlockCategory? =
            entries.firstOrNull { it.value == value }
    }
}

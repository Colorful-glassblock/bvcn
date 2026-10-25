package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.sponsorblock.SponsorSegment
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * BilibiliSponsorBlock 接口。
 *
 * 文档：https://github.com/hanydd/BilibiliSponsorBlock/wiki/API
 * 与 B 站接口无关，所以单独建 client，不复用 BiliHttpApi 的签名和 UA 逻辑。
 */
@Single
class SponsorBlockRepository {
    private val logger = KotlinLogging.logger("SponsorBlock")

    /** 默认使用社区公共实例，可在设置里改 */
    var server: String = DEFAULT_SERVER

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    /**
     * 查询某个视频的片段列表。
     *
     * 没提交过片段时服务器返回空数组；网络或解析失败返回空列表，不影响播放。
     */
    suspend fun getSkipSegments(bvid: String, cid: Long): List<SponsorSegment> {
        if (bvid.isBlank()) return emptyList()
        return runCatching {
            val url = "${server.trimEnd('/')}/api/skipSegments"
            val body = client.get(url) {
                parameter("videoID", bvid)
                parameter("cid", cid)
            }.bodyAsText()
            if (body.isBlank()) return@runCatching emptyList()
            // 服务器在无数据时可能返回 "{}" 或 "[]"
            val segments = json.decodeFromString<List<SponsorSegment>>(body)
            logger.info { "SponsorBlock segments for $bvid: ${segments.size}" }
            segments
        }.onFailure {
            logger.warn { "Fetch sponsor segments failed: ${it.localizedMessage}" }
        }.getOrDefault(emptyList())
    }

    /**
     * 上报已看过某个片段，用于社区统计。
     */
    suspend fun viewedSegment(uuid: String) {
        if (uuid.isBlank()) return
        runCatching {
            client.get("${server.trimEnd('/')}/api/viewedVideoSponsorTime") {
                parameter("UUID", uuid)
            }
        }.onFailure {
            logger.debug { "Report viewed segment failed: ${it.localizedMessage}" }
        }
    }

    companion object {
        const val DEFAULT_SERVER = "https://www.bsbsb.top"

        /** 常见备选地址，供设置页展示 */
        val KNOWN_SERVERS = listOf(
            DEFAULT_SERVER,
        )
    }
}

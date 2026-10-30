package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.network.entity.Release
import dev.aaa1115910.bv.util.Prefs
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

object GithubApi {
    private var endPoint = "api.github.com"
    private const val OWNER = "Colorful-glassblock"
    private const val REPO = "bvcn"
    // 当前构建所在分支，用于匹配对应分支的 release
    private const val CURRENT_BRANCH = "develop"
    // 当前分支 CI 产物使用的 tag 前缀（alpha-r* / lastAndroid5-r*）
    private const val RELEASE_TAG_PREFIX = "alpha-"
    // 正式版 release 使用 v* tag
    private const val STABLE_RELEASE_TAG_PREFIX = "v"
    private const val RELEASES_PER_PAGE = 30
    private const val MAX_RELEASE_PAGES = 10

    /** 成功结果的缓存有效期：GitHub 未认证接口每小时只有 60 次配额，不能每次进页面都打 */
    private const val CACHE_TTL_MS = 30 * 60 * 1000L
    /** 失败后的冷却时间，避免配额耗尽时反复重试把日志刷爆 */
    private const val FAILURE_COOLDOWN_MS = 10 * 60 * 1000L

    private class CacheEntry(val release: Release, val timestamp: Long)

    private var cachedRelease: CacheEntry? = null
    private var lastFailureAt = 0L
    private var lastFailureMessage: String? = null
    private val requestMutex = Mutex()
    private lateinit var client: HttpClient
    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val isDebug get() = BuildConfig.DEBUG
    private val isAlpha get() = Prefs.updateAlpha

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BrowserUserAgent()
            install(ContentNegotiation) {
                json(json)
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = endPoint
                }
            }
        }
    }

    /**
     * 获取仓库所有 release，不附加任何过滤
     */
    private suspend fun getAllReleases(
        owner: String = OWNER,
        repo: String = REPO,
        pageSize: Int = RELEASES_PER_PAGE,
        page: Int = 1
    ): List<Release> {
        val response = client.get("repos/$owner/$repo/releases") {
            parameter("per_page", pageSize)
            parameter("page", page)
        }.bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString<List<Release>>(response)
    }

    /**
     * 按发布时间从新到旧分页查找第一条满足条件的 release。
     *
     * 这里必须根据“过滤前”的整页大小判断是否继续翻页：develop 和 lastAndroid5
     * 的 release 是混在一起的，某一页可能正好全是另一个分支的包，过滤后为空
     * 并不代表后面没有当前分支的 release。
     */
    private suspend fun findLatestRelease(
        owner: String = OWNER,
        repo: String = REPO,
        predicate: (Release) -> Boolean
    ): Release? {
        var page = 1
        while (page <= MAX_RELEASE_PAGES) {
            val releases = getAllReleases(owner, repo, page = page)
            releases.firstOrNull(predicate)?.let { return it }
            if (releases.size < RELEASES_PER_PAGE) break
            page++
        }
        return null
    }

    private suspend fun getLatestRelease(
        owner: String = OWNER,
        repo: String = REPO
    ): Release = findLatestRelease(owner, repo) {
        it.isRelease && it.tagName.startsWith(STABLE_RELEASE_TAG_PREFIX)
    } ?: throw IllegalStateException("No release found")

    suspend fun getLatestPreReleaseBuild(): Release =
        findLatestRelease {
            it.isPreRelease && it.tagName.startsWith(RELEASE_TAG_PREFIX)
        } ?: throw IllegalStateException("No pre-release found for branch $CURRENT_BRANCH")

    suspend fun getLatestReleaseBuild(): Release = getLatestRelease()

    /**
     * 获取当前渠道的最新构建。
     *
     * 带 30 分钟缓存与失败冷却：调用方可能在同一页面重复触发（进入设置页、打开弹窗各一次），
     * 而 GitHub 未认证 API 每小时只有 60 次配额，直接每次都请求会很快被打爆。
     *
     * @param force 忽略缓存强制刷新
     */
    suspend fun getLatestBuild(force: Boolean = false): Release = requestMutex.withLock {
        val now = System.currentTimeMillis()

        if (!force) {
            cachedRelease?.takeIf { now - it.timestamp < CACHE_TTL_MS }?.let {
                return@withLock it.release
            }
            // 刚失败过就直接抛上次的错误，不再消耗配额
            lastFailureMessage?.let { message ->
                if (now - lastFailureAt < FAILURE_COOLDOWN_MS) {
                    throw IllegalStateException(message)
                }
            }
        }

        try {
            val release = if (isAlpha) getLatestPreReleaseBuild() else getLatestReleaseBuild()
            cachedRelease = CacheEntry(release, now)
            lastFailureMessage = null
            release
        } catch (e: Exception) {
            lastFailureAt = now
            lastFailureMessage = e.message ?: "检查更新失败"
            throw e
        }
    }

    /** 清空缓存，供设置页「重新检查」使用 */
    fun invalidateCache() {
        cachedRelease = null
        lastFailureMessage = null
        lastFailureAt = 0L
    }

    private fun checkErrorMessage(data: String) {
        val responseElement = json.parseToJsonElement(data)
        if (responseElement !is JsonObject) return
        val responseObject = responseElement.jsonObject
        check(responseObject["message"] == null) { responseObject["message"]!!.jsonPrimitive.content }
    }

    suspend fun downloadUpdate(
        release: Release,
        file: File,
        downloadListener: ProgressListener
    ) {
        val downloadUrl =
            if (isDebug) release.assets.firstOrNull { it.name.contains("debug") }?.browserDownloadUrl
            else release.assets.firstOrNull { it.name.contains("alpha") || it.name.contains("release") }?.browserDownloadUrl
        downloadUrl ?: throw IllegalStateException("Didn't find download url")
        client.prepareRequest {
            url(downloadUrl)
            onDownload(downloadListener)
        }.execute { response ->
            response.bodyAsChannel().copyAndClose(file.writeChannel())
        }
    }
}

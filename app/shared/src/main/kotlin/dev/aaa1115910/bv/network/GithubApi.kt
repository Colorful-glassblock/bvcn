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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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
        pageSize: Int = 30,
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
     * 解析 sever git ref 返回的 JSON 提取 HEAD commit SHA
     * 用于获取某个分支的最新 commit hash
     */
    private suspend fun getBranchHeadSha(branch: String): String {
        val response = client.get("repos/$OWNER/$REPO/git/refs/heads/$branch") {
            parameter("per_page", 1)
        }.bodyAsText()
        checkErrorMessage(response)
        val responseObject: JsonObject = json.parseToJsonElement(response).jsonObject
        return responseObject["object"]!!.jsonObject["sha"]!!.jsonPrimitive.content
    }

    /**
     * 获取指定分支的 release 列表。
     * 通过比对 release.targetCommitish 与分支 HEAD SHA 过滤。
     */
    private suspend fun getReleases(
        owner: String = OWNER,
        repo: String = REPO,
        pageSize: Int = 30,
        page: Int = 1
    ): List<Release> {
        // 先 resolve 当前分支 HEAD commit SHA，用作过滤条件
        val branchHeadSha = try {
            getBranchHeadSha(CURRENT_BRANCH)
        } catch (e: Exception) {
            // 网络或 API 错误时降级：不分支过滤，直接返回所有 release
            return getAllReleases(owner, repo, pageSize, page)
        }
        val allReleases = getAllReleases(owner, repo, pageSize, page)
        // 只保留 targetCommitish 精确匹配当前分支 HEAD SHA 的 release
        return allReleases.filter { it.targetCommitish == branchHeadSha }
    }

    private suspend fun getLatestRelease(
        owner: String = OWNER,
        repo: String = REPO
    ): Release {
        val releases = getReleases(owner = owner, repo = repo, pageSize = 1)
        return releases.firstOrNull { !it.isPreRelease }
            ?: throw IllegalStateException("No release found for branch $CURRENT_BRANCH")
    }

    suspend fun getLatestPreReleaseBuild(): Release {
        var release: Release? = null
        var page = 1
        while (release == null && page <= 10) {
            val releases = getReleases(page = page)
            if (releases.isEmpty()) break
            release = releases.firstOrNull { it.isPreRelease }
            page++
        }
        return release ?: throw IllegalStateException("No pre-release found for branch $CURRENT_BRANCH")
    }

    suspend fun getLatestReleaseBuild(): Release = getLatestRelease()

    suspend fun getLatestBuild(): Release =
        if (isAlpha) getLatestPreReleaseBuild() else getLatestReleaseBuild()

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

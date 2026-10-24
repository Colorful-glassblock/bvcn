package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.util.LogCatcherUtil
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object HttpServer {
    var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun startServer() {
        GlobalScope.launch(Dispatchers.IO) {
            server = embeddedServer(CIO, port = 0) {
                homeModule()
                logsApiModule()
            }
            server?.start(wait = true)
        }
    }

    private fun Application.homeModule() {
        routing {
            get("/") {
                call.respondText("Hello World!")
            }
        }
    }

    private fun Application.logsApiModule() {
        routing {
            get("/api/logs/{filename}") {
                val filename =
                    call.parameters["filename"] ?: return@get call.respondText(
                        text = "filename is null",
                        status = HttpStatusCode.NotFound
                    )
                LogCatcherUtil.updateLogFiles()
                val file = (LogCatcherUtil.crashFiles + LogCatcherUtil.manualFiles)
                    .find { it.name == filename } ?: return@get call.respondText(
                    text = "file not found",
                    status = HttpStatusCode.NotFound
                )
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        // 文件名里的冒号会让部分 Android 下载器和桌面浏览器保存失败
                        file.name.replace(':', '-')
                    ).toString()
                )
                call.respondFile(file)
            }
        }
    }
}
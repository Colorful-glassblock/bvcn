package dev.aaa1115910.bv.mobile.screen.settings.details

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.mobile.component.preferences.SwitchPreferenceItem
import dev.aaa1115910.bv.mobile.component.preferences.TextPreferenceItem
import dev.aaa1115910.bv.util.PrefKeys
import dev.aaa1115910.bv.util.Prefs

@Composable
fun NetworkContent(
    modifier: Modifier = Modifier
) {
    var enableProxy by remember { mutableStateOf(Prefs.enableProxy) }
    var proxyHttpServer by remember { mutableStateOf(Prefs.proxyHttpServer) }
    var proxyGRPCServer by remember { mutableStateOf(Prefs.proxyGRPCServer) }
    var editingGrpc by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }

    if (editingGrpc) {
        AlertDialog(
            onDismissRequest = { editingGrpc = false },
            confirmButton = {
                TextButton(onClick = {
                    proxyGRPCServer = editValue
                    Prefs.proxyGRPCServer = editValue
                    editingGrpc = false
                }) { Text(text = "确定") }
            },
            dismissButton = {
                TextButton(onClick = { editingGrpc = false }) { Text(text = "取消") }
            },
            title = { Text(text = "GRPC 代理服务器") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    singleLine = true,
                    placeholder = { Text(text = "host:port") }
                )
            }
        )
    }

    Column(modifier = modifier) {
        SwitchPreferenceItem(
            title = "启用网络代理",
            summary = "通过代理服务器获取港澳台等地区内容",
            checked = enableProxy,
            onCheckedChange = { enable ->
                enableProxy = enable
                Prefs.enableProxy = enable
                if (enable) BVApp.instance?.initProxy()
            }
        )
        if (enableProxy) {
            TextPreferenceItem(
                title = "GRPC 代理服务器",
                summary = if (proxyGRPCServer.isBlank()) "未设置" else proxyGRPCServer,
                onClick = {
                    editValue = proxyGRPCServer
                    editingGrpc = true
                }
            )
            TextPreferenceItem(
                title = "HTTP 代理服务器",
                summary = if (proxyHttpServer.isBlank()) "未设置" else proxyHttpServer
            )
        }
        SwitchPreferenceItem(
            title = "优先使用官方 CDN",
            summary = "国内网络下优先使用 B 站官方 CDN",
            prefReq = PrefKeys.prefPreferOfficialCdnRequest
        )
    }
}

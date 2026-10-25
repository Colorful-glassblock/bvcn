package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.mobile.component.preferences.items.listItemPreference
import dev.aaa1115910.bv.mobile.component.preferences.items.switchPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
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

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
            "代理" to {
                switchPreference(
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
                    listItemPreference(
                        title = "GRPC 代理服务器",
                        summary = if (proxyGRPCServer.isBlank()) "未设置" else proxyGRPCServer,
                        onClick = {
                            editValue = proxyGRPCServer
                            editingGrpc = true
                        }
                    )
                    listItemPreference(
                        title = "HTTP 代理服务器",
                        summary = if (proxyHttpServer.isBlank()) "未设置" else proxyHttpServer
                    )
                }
            },
            "CDN" to {
                switchPreference(
                    title = "优先使用官方 CDN",
                    summary = "国内网络下优先使用 B 站官方 CDN",
                    prefReq = dev.aaa1115910.bv.util.PrefKeys.prefPreferOfficialCdnRequest,
                    onCheckedChange = { true }
                )
            }
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NetworkContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            NetworkContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

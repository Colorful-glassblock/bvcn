package dev.aaa1115910.bv.mobile.activities

import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.aaa1115910.bv.mobile.screen.MobileMainScreen
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 检测是否在 TV 上运行，如果是则跳转到 TV 版
        if (isTvDevice()) {
            try {
                val tvMainActivity = Intent().apply {
                    setClassName(
                        this@MainActivity,
                        "dev.aaa1115910.bv.tv.activities.MainActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(tvMainActivity)
                finish()
                return
            } catch (e: Exception) {
                // TV 版 Activity 不存在，继续使用手机版
            }
        }

        var keepSplashScreen = true
        installSplashScreen().apply {
            setKeepOnScreenCondition { keepSplashScreen }
        }
        super.onCreate(savedInstanceState)

        setContent {
            keepSplashScreen = false

            BVMobileTheme {
                MobileMainScreen()
            }
        }
    }

    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
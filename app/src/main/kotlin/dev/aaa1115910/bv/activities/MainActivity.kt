package dev.aaa1115910.bv.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.aaa1115910.bv.repository.UserRepository
<<<<<<< HEAD:app/src/main/kotlin/dev/aaa1115910/bv/activities/MainActivity.kt
import dev.aaa1115910.bv.screen.MainScreen
import dev.aaa1115910.bv.screen.user.lock.UnlockUserScreen
=======
import dev.aaa1115910.bv.tv.screens.MainScreen
import dev.aaa1115910.bv.tv.screens.user.lock.UnlockUserScreen
>>>>>>> 9be90bc5 (Remove mainland China detection and allow unrestricted access):app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/activities/MainActivity.kt
import dev.aaa1115910.bv.ui.theme.BVTheme
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val userRepository: UserRepository by inject()
    private val logger = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplashScreen = true
        installSplashScreen().apply {
            setKeepOnScreenCondition { keepSplashScreen }
        }
        super.onCreate(savedInstanceState)

        setContent {
            var isCheckingUserLock by remember { mutableStateOf(true) }
            var userLockLocked by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val user = userRepository.findUserByUid(userRepository.uid)
                userLockLocked = user?.lock?.isNotBlank() ?: false
                logger.info { "default user: ${user?.username}" }
                isCheckingUserLock = false
                keepSplashScreen = false
            }

            BVTheme {
                if (isCheckingUserLock) {
                    //避免在检查用户锁的期间加载屏幕内容
                } else {
                    if (!userLockLocked) {
                        MainScreen()
                    } else {
                        UnlockUserScreen(
                            onUnlockSuccess = { user ->
                                logger.info { "unlock user lock for user ${user.uid}" }
                                userLockLocked = false
                            }
                        )
                    }
                }
            }
        }
    }
}


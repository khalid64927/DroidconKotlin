package co.touchlab.droidcon.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import co.touchlab.droidcon.R
import co.touchlab.droidcon.android.ui.main.Main
import co.touchlab.droidcon.android.ui.theme.DroidconTheme
import co.touchlab.droidcon.android.viewModel.MainViewModel
import co.touchlab.droidcon.application.service.NotificationSchedulingService
import co.touchlab.droidcon.domain.service.AnalyticsService
import co.touchlab.droidcon.domain.service.SyncService
import co.touchlab.kermit.Logger
import com.google.accompanist.insets.ProvideWindowInsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class MainActivity: ComponentActivity(), KoinComponent {
    private val notificationSchedulingService: NotificationSchedulingService by inject()
    private val syncService: SyncService by inject()
    private val log: Logger by inject { parametersOf("MainActivity") }
    private val analyticsService: AnalyticsService by inject()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()

        analyticsService.logEvent(AnalyticsService.EVENT_STARTED)

        lifecycleScope.launchWhenCreated {
            notificationSchedulingService.runScheduling()
        }

        lifecycleScope.launchWhenCreated {
            syncService.runSynchronization()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DroidconTheme {
                ProvideWindowInsets {
                    Surface(color = MaterialTheme.colors.primary, modifier = Modifier.fillMaxSize()) {
                        Main(main = mainViewModel)
                    }

                    val showSplashScreen by mainViewModel.showSplashScreen.collectAsState()
                    Crossfade(targetState = showSplashScreen) { showSplashScreen ->
                        if (showSplashScreen) {
                            LaunchedEffect(mainViewModel) {
                                mainViewModel.didShowSplashScreen()
                            }
                            Surface(color = MaterialTheme.colors.primary, modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_splash_screen),
                                    contentDescription = getString(R.string.droidcon_title),
                                    modifier = Modifier
                                        .padding(32.dp)
                                        .fillMaxSize(0.75f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.initializeFeedbackObserving()
    }
}

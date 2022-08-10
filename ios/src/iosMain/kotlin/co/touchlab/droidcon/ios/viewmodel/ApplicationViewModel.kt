package co.touchlab.droidcon.ios.viewmodel

import co.touchlab.droidcon.application.gateway.SettingsGateway
import co.touchlab.droidcon.application.service.NotificationSchedulingService
import co.touchlab.droidcon.application.service.NotificationService
import co.touchlab.droidcon.domain.service.FeedbackService
import co.touchlab.droidcon.domain.service.SyncService
import co.touchlab.droidcon.ios.viewmodel.session.AgendaViewModel
import co.touchlab.droidcon.ios.viewmodel.session.ScheduleViewModel
import co.touchlab.droidcon.ios.viewmodel.settings.SettingsViewModel
import co.touchlab.droidcon.ios.viewmodel.sponsor.SponsorListViewModel
import co.touchlab.droidcon.service.NotificationHandler
import org.brightify.hyperdrive.multiplatformx.BaseViewModel

class ApplicationViewModel(
    scheduleFactory: ScheduleViewModel.Factory,
    agendaFactory: AgendaViewModel.Factory,
    sponsorsFactory: SponsorListViewModel.Factory,
    settingsFactory: SettingsViewModel.Factory,
    private val feedbackDialogFactory: FeedbackDialogViewModel.Factory,
    private val syncService: SyncService,
    private val notificationSchedulingService: NotificationSchedulingService,
    private val feedbackService: FeedbackService,
    private val settingsGateway: SettingsGateway,
): BaseViewModel(), NotificationHandler {

    val schedule by managed(scheduleFactory.create())
    val agenda by managed(agendaFactory.create())
    val sponsors by managed(sponsorsFactory.create())
    val settings by managed(settingsFactory.create())

    val useCompose by collected(settingsGateway.settings()) { it.useComposeForIos }

    var presentedFeedback: FeedbackDialogViewModel? by managed(null)

    init {
        lifecycle.whileAttached {
            notificationSchedulingService.runScheduling()
        }

        lifecycle.whileAttached {
            syncService.runSynchronization()
        }
    }

    override fun notificationReceived(sessionId: String, notificationType: NotificationService.NotificationType) {
        if (notificationType == NotificationService.NotificationType.Feedback) {
            lifecycle.whileAttached {
                // We're not checking whether feedback is enabled, because the user opened a feedback notification.
                presentNextFeedback()
            }
        }
    }

    fun onAppear() {
        lifecycle.whileAttached {
            if (settingsGateway.settings().value.isFeedbackEnabled) {
                presentNextFeedback()
            }
        }
    }

    private suspend fun presentNextFeedback() {
        presentedFeedback = feedbackService.next()?.let { session ->
            feedbackDialogFactory.create(
                session,
                submit = { feedback ->
                    feedbackService.submit(session, feedback)
                    presentNextFeedback()
                },
                closeAndDisable = {
                    settingsGateway.setFeedbackEnabled(false)
                    presentedFeedback = null
                },
                skip = {
                    feedbackService.skip(session)
                    presentNextFeedback()
                },
            )
        }
    }
}

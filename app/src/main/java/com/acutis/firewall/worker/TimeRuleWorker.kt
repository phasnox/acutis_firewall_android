package com.acutis.firewall.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.acutis.firewall.data.repository.TimeRuleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@HiltWorker
class TimeRuleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val timeRuleRepository: TimeRuleRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Reset daily usage at midnight
            timeRuleRepository.resetDailyUsage()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "time_rule_daily_reset"

        fun scheduleDailyReset(context: Context) {
            val currentTime = LocalTime.now()
            val midnight = LocalTime.MIDNIGHT

            // Calculate delay until next midnight
            val delayMinutes = if (currentTime.isBefore(midnight)) {
                java.time.Duration.between(currentTime, midnight).toMinutes()
            } else {
                java.time.Duration.between(currentTime, midnight.plusHours(24)).toMinutes()
            }

            val workRequest = PeriodicWorkRequestBuilder<TimeRuleWorker>(
                24, TimeUnit.HOURS
            )
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
}

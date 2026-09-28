package de.dgstudios.spinkingdom.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.dgstudios.spinkingdom.MainActivity
import de.dgstudios.spinkingdom.R
import de.dgstudios.spinkingdom.storage.DataStoreGameStateRepository
import java.util.concurrent.TimeUnit

object Notifications {
    const val CHANNEL = "spins_full"
    private const val WORK = "spins_full_reminder"

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun hasPermission(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Schedules a local reminder for when the spins are refilled. */
    fun scheduleSpinsFull(context: Context, delayMs: Long) {
        val req = OneTimeWorkRequestBuilder<SpinsFullWorker>().setInitialDelay(delayMs.coerceAtLeast(60_000L), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    fun show(context: Context) {
        if (!hasPermission(context)) return
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(1, n) }
    }
}

class SpinsFullWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val state = DataStoreGameStateRepository.create(applicationContext).load()
        if (state.settings.notifications) Notifications.show(applicationContext)
        return Result.success()
    }
}

package com.kerimovscreations.naturun.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kerimovscreations.naturun.R
import com.kerimovscreations.naturun.activities.MainActivity
import android.net.Uri


class NotificationService private constructor() {

    // singleton

    companion object {
        val instance = NotificationService()
    }

    /**
     * Variables
     */

    private var notificationId: Int = 1
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManagerCompat? = null

    fun setupNotification(context: Context) {
        if (notificationBuilder == null)
            initNotificationBuilder(context)

        notificationBuilder!!
            .setContentTitle("Warning!")
            .setContentText("Around wild animals")
            .setOngoing(false)

        val sound =
            Uri.parse("android.resource://" + context.packageName + "/" + R.raw.notification_sound);//Here is FILE_NAME is the name of file that you want to play


        notificationBuilder?.setSound(sound)
        notificationManager?.notify(
            notificationId,
            notificationBuilder!!.build()
        )
    }

    private fun initNotificationBuilder(context: Context) {
        val intent = Intent(context, MainActivity::class.java)

        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)


        val CHANNEL_ID = "Naturun"

        createNotificationChannel(context, CHANNEL_ID)

        notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setColor(ContextCompat.getColor(context, R.color.yellow))
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager = NotificationManagerCompat.from(context)
    }

    private fun createNotificationChannel(context: Context, channelId: String) {
        val sound =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.notification_sound)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.enableVibration(true)
            channel.setSound(sound, attributes)
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
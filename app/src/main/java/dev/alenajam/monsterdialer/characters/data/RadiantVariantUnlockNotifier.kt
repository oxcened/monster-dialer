package dev.alenajam.monsterdialer.characters.data

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
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.MainActivity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Posts a persistent reminder after a player discovers a radiant variant. */
@Singleton
class RadiantVariantUnlockNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shareCardFactory: RadiantUnlockShareCardFactory,
) {
    suspend fun show(characterName: String, frontSpritePath: String?) = withContext(Dispatchers.IO) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return@withContext
        createChannel()
        val openApp = PendingIntent.getActivity(
            context,
            characterName.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.radiant_variant_unlocked_title))
            .setContentText(context.getString(R.string.radiant_variant_unlocked_message, characterName))
            .setContentIntent(openApp)
            .setAutoCancel(true)
        frontSpritePath?.let { path ->
            shareCardFactory.create(characterName, path)?.let { imageUri ->
                notification.addAction(
                    0,
                    context.getString(R.string.share),
                    sharePendingIntent(imageUri),
                )
            }
        }
        NotificationManagerCompat.from(context).notify(characterName.hashCode(), notification.build())
    }

    private fun sharePendingIntent(imageUri: android.net.Uri): PendingIntent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            clipData = android.content.ClipData.newRawUri("radiant-unlock.png", imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.radiant_unlock_share_title))
        return PendingIntent.getActivity(
            context,
            imageUri.hashCode(),
            chooser,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ChannelId,
            context.getString(R.string.radiant_unlock_notifications),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val ChannelId = "radiant_variant_unlocks"
    }
}

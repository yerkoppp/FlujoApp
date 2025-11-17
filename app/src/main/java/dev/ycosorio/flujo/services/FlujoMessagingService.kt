package dev.ycosorio.flujo.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.ycosorio.flujo.MainActivity
import dev.ycosorio.flujo.R
import kotlin.random.Random
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

class FlujoMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extraer datos del mensaje
        val title = remoteMessage.notification?.title ?: "Flujo"
        val body = remoteMessage.notification?.body ?: ""
        val data = remoteMessage.data

        // ✅ Verificar que no sea el remitente ANTES de mostrar
        val currentUser = FirebaseAuth.getInstance().currentUser
        val senderId = data["senderId"]

        if (currentUser != null && senderId == currentUser.uid) {
            Timber.tag("FlujoMessagingService")
                .d("🚫 No mostrar notificación propia del remitente")
            return
        }

        // Mostrar notificación solo si NO es el remitente
        showNotification(title, body)

        // Guardar en Firestore
        saveNotificationToFirestore(title, body, data)
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "flujo_notifications"

        // Crear canal de notificación (necesario para Android 8+)
        val channel = NotificationChannel(
            channelId,
            "Notificaciones de Flujo",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones generales de la aplicación"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent para abrir la app cuando se toca la notificación
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openNotifications", true)
            putExtra("notificationTitle", title)
            putExtra("notificationBody", message)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.small_icon) // Cambia por tu icono
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
    private fun saveNotificationToFirestore(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        try {
            // Obtener el usuario actual (quien RECIBE la notificación)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Timber.tag("FlujoMessagingService")
                    .w("No hay usuario autenticado, no se guarda notificación")
                return
            }

            val userId = currentUser.uid

            // Verificar que no sea el remitente (evitar guardar notificación propia)
            val senderId = data["senderId"]
            if (senderId == userId) {
                Timber.tag("FlujoMessagingService")
                    .d("No guardar notificación propia del remitente")
                return
            }

            // Crear documento de notificación
            val notificationData = hashMapOf(
                "userId" to userId,
                "title" to title,
                "body" to body,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "isRead" to false,
                "type" to (data["type"] ?: "message"),
                "data" to data
            )

            // Guardar en Firestore
            FirebaseFirestore.getInstance()
                .collection("notifications")
                .add(notificationData)
                .addOnSuccessListener {
                    Timber.tag("FlujoMessagingService").d("✅ Notificación guardada en Firestore")
                }
                .addOnFailureListener { e ->
                    Timber.tag("FlujoMessagingService").e(e, "❌ Error al guardar notificación")
                }
        } catch (e: Exception) {
            Timber.tag("FlujoMessagingService").e(e, "❌ Error al procesar notificación")
        }
    }
}

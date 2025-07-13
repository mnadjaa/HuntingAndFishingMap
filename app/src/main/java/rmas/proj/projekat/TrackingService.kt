package rmas.proj.projekat

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import java.io.IOException
import android.os.Build


class TrackingService : Service() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val serverUrl = "https://tvoj.server.com/api/location" // Zameni sa tvojim URL-om⁉️⁉️⁉️⁉️⁉️

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(1, createNotification("Praćenje lokacije aktivno"))
        requestLocationUpdates()
        subscribeToNotifications()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 sekundi
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        sendLocationToServer(location.latitude, location.longitude)
                    }
                }
            }, null)
        }
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("latitude", lat.toString())
            .add("longitude", lon.toString())
            .build()

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TrackingService", "Neuspešno slanje lokacije", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("TrackingService", "Lokacija uspešno poslata")
            }
        })
    }

    private fun subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("lokacija")
    }

    private fun createNotification(content: String): Notification {
        val channelId = "tracking_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if the device is running Android O (API 26) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Praćenje lokacije",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Lokacija")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW) // This replaces channel importance for older devices
            .build()
    }
}

package rmas.proj.projekat

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import rmas.proj.projekat.data.LocationType

class TrackingService : Service() {
    companion object { // = static
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1
        const val PROXIMITY_NOTIFICATION_ID = 2
        const val PROXIMITY_RADIUS_KM = 0.5 //sve na blizini do 500m
        var isRunning = false
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastNotificationTime = 0L
    private val NOTIFICATION_COOLDOWN_MS = 60_000L // jedna notifikacija po minutu max

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY //ako ga android ubije pokrece se opet automatski
    }

    override fun onDestroy() { //gasenje servisa
        super.onDestroy()
        isRunning = false
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null //ne povezujem ga sa nekim delom app samo preko njega mogu da je pokrenem

    // --- Lokacija ---
    private fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            interval = 30_000L
            fastestInterval = 15_000L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    saveLocationToFirestore(latLng)
                    checkNearbyLocations(latLng)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("TrackingService", "Nema dozvole za lokaciju", e)
            stopSelf()
        }
    }

    // cuvanje lokacije korisnika
    private fun saveLocationToFirestore(latLng: LatLng) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val data = mapOf(
            "position" to hashMapOf(
                "latitude" to latLng.latitude,
                "longitude" to latLng.longitude
            ),
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("userLocations")
            .document(userId)
            .set(data)
            .addOnFailureListener { e ->
                Log.e("TrackingService", "Greška pri čuvanju lokacije", e)
            }
    }

    //proverava lokacije u blizini
    private fun checkNearbyLocations(userLatLng: LatLng) {
        db.collection("locations")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val positionMap = doc.get("position") as? Map<String, Any> ?: continue
                        val lat = positionMap["latitude"] as? Double ?: continue
                        val lng = positionMap["longitude"] as? Double ?: continue
                        val name = doc.getString("name") ?: "Nepoznata lokacija"
                        val typeStr = doc.getString("type") ?: continue
                        val type = try {
                            LocationType.valueOf(typeStr)
                        } catch (e: Exception) {
                            continue
                        }

                        val distance = distanceBetween(
                            userLatLng.latitude, userLatLng.longitude,
                            lat, lng
                        )

                        if (distance <= PROXIMITY_RADIUS_KM) {
                            val now = System.currentTimeMillis()
                            if (now - lastNotificationTime > NOTIFICATION_COOLDOWN_MS) {
                                lastNotificationTime = now
                                showProximityNotification(name, type, distance)
                            }
                            break
                        }
                    } catch (e: Exception) {
                        Log.e("TrackingService", "Greška pri parsiranju lokacije", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TrackingService", "Greška pri čitanju lokacija", e)
            }
    }

    // Notifikacije
    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Praćenje aktivno")
            .setContentText("Servis prati vašu lokaciju")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showProximityNotification(
        locationName: String,
        type: LocationType,
        distanceKm: Double
    ) {
        val typeLabel = when (type) {
            LocationType.HUNTING -> "mesto za lov"
            LocationType.FISHING -> "mesto za ribolov"
        }
        val distanceM = (distanceKm * 1000).toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, PROXIMITY_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pronađeno $typeLabel u blizini!")
            .setContentText("$locationName — ${distanceM}m od vas")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(PROXIMITY_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Praćenje lokacije",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kanal za obaveštenja o praćenju lokacije"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun distanceBetween(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000.0
    }
}

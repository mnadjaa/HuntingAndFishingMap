package rmas.proj.projekat.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Funkcija za dobijanje trenutne lokacije
@SuppressLint("MissingPermission")
fun getCurrentLocation(
    fusedLocationProvider: FusedLocationProviderClient,
    onLocationReceived: (LatLng) -> Unit
) {
    fusedLocationProvider.lastLocation
        .addOnSuccessListener { location ->
            location?.let {
                onLocationReceived(LatLng(it.latitude, it.longitude))
            }
        }
        .addOnFailureListener {
            println("Nije uspelo pribavljanje lokacije")//Nije moguće dobiti lokaciju
        }
}

suspend fun uploadLocationImage(context: Context, imageUri: Uri): String {//🐛!!!!jel mofu da koristim onu iz posebne klase sto imam
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "temp_location_image.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val cloudinary = Cloudinary(//🐛ovo promeni da se ucitava iz config fajla
                ObjectUtils.asMap(
                    "cloud_name", "dsoyvcupj",
                    "api_key", "766928287421829",
                    "api_secret", "DVFDtQ6PRj3K4cJLsaej8Z-_IRw"
                )
            )
            val result = cloudinary.uploader().upload(file, ObjectUtils.emptyMap())
            result["secure_url"] as String
        } catch (e: Exception) {
            throw e
        }
    }
}

fun formatTimestamp(timestamp: Long): String {//🐛ovo treba da se koristi ali ne znam da li ga koristim
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@SuppressLint("MissingPermission")
fun setupLocationUpdates(
    fusedLocationProvider: FusedLocationProviderClient,
    locationCallback: LocationCallback
) {
    val locationRequest = LocationRequest.create().apply {
        interval = 5000  // Update every 5 seconds
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    fusedLocationProvider.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
    )
}


// Function to mock user location (requires Location Edit app or another mock location app)
// Function to mock user location using Loca Edit app
fun mockUserLocation(context: Context, position: LatLng) {
    try {
        // Create intent to communicate with Loca Edit
        val intent = Intent("com.miramire.locaedit.SET_LOCATION")
        intent.putExtra("latitude", position.latitude)
        intent.putExtra("longitude", position.longitude)
        intent.putExtra("accuracy", 3.0f) // Accuracy in meters

        // Try to send the intent
        context.sendBroadcast(intent)

        /*Toast.makeText(
            context,
            "Lokacija postavljena na: ${position.latitude}, ${position.longitude}",
            Toast.LENGTH_SHORT
        ).show()*/
    } catch (e: Exception) {
        Log.e("MockLocation", "Error sending location to Loca Edit: ${e.message}")
        Toast.makeText(
            context,
            "Nije moguće promeniti lokaciju. Proverite da li je Loca Edit aplikacija instalirana.",
            Toast.LENGTH_LONG
        ).show()
    }
}

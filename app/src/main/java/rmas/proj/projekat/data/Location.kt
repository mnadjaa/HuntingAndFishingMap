package rmas.proj.projekat.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Location(
    @DocumentId var id: String = "",          // Ovaj ID je automatski dodeljen od strane Firestore-a
    val name: String,
    val description: String,
    val type: LocationType,
    val position: LatLng,
    val photos: List<String>,
    val authorId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastInteraction: Long = System.currentTimeMillis(),
    val ratings: List<Rating> = emptyList()
)


class LocationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val locationsCollection = db.collection("locations")

    suspend fun createLocation(location: Location): Result<Location> = kotlin.runCatching {
        // Create a new document reference
        val locationDoc = locationsCollection.document()

        val timestamp = System.currentTimeMillis()

        // Create location with the generated ID
        val locationWithId = location.copy(
            id = locationDoc.id,
            photos = location.photos,//🆕🆕🆕🆕🆕🆕🆕🆕🆕 pre je bilo uniquePhotos,
            createdAt =timestamp,// System.currentTimeMillis(),
            lastInteraction = timestamp//System.currentTimeMillis()
        )

        // Convert Location to HashMap for Firestore
        val locationData = hashMapOf(
            "id" to locationWithId.id,
            "name" to locationWithId.name,
            "description" to locationWithId.description,
            "type" to locationWithId.type.name,
            "position" to hashMapOf(
                "latitude" to locationWithId.position.latitude,
                "longitude" to locationWithId.position.longitude
            ),
            "authorId" to locationWithId.authorId,
            "photos" to locationWithId.photos,
            "ratings" to locationWithId.ratings.map { rating ->
                hashMapOf(
                    "userId" to rating.userId,
                    "score" to rating.score,
                    "comment" to rating.comment,
                    "timestamp" to rating.timestamp
                )
            },
            "createdAt" to locationWithId.createdAt,
            "lastInteraction" to locationWithId.lastInteraction
        )

        // Save to Firestore
        locationDoc.set(locationData).await()
        locationWithId
    }

    suspend fun getAllLocations(): Result<List<Location>> = kotlin.runCatching {
        val snapshot = locationsCollection.get().await()
        snapshot.documents.mapNotNull { document ->
            try {
                val data = document.data ?: return@mapNotNull null

                // Extract position data
                val positionMap = data["position"] as? Map<*, *>
                val position = if (positionMap != null) {
                    LatLng(
                        positionMap["latitude"] as Double,
                        positionMap["longitude"] as Double
                    )
                } else {
                    return@mapNotNull null
                }

                // Extract ratings
                val ratingsData = data["ratings"] as? List<*>
                val ratings = ratingsData?.mapNotNull { ratingData ->
                    (ratingData as? Map<*, *>)?.let { rating ->
                        Rating(
                            userId = rating["userId"] as? String ?: return@mapNotNull null,
                            score = (rating["score"] as? Double)?.toFloat() ?: return@mapNotNull null,
                            comment = rating["comment"] as? String ?: return@mapNotNull null,
                            timestamp = rating["timestamp"] as? Long ?: System.currentTimeMillis()
                        )
                    }
                } ?: emptyList()

                Location(
                    id = document.id,
                    name = data["name"] as? String ?: return@mapNotNull null,
                    description = data["description"] as? String ?: return@mapNotNull null,
                    type = LocationType.valueOf(data["type"] as? String ?: return@mapNotNull null),
                    position = position,
                    authorId = data["authorId"] as? String ?: return@mapNotNull null,
                    photos = (data["photos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    ratings = ratings,
                    createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                    lastInteraction = data["lastInteraction"] as? Long ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e("LocationRepository", "Error parsing location document: ${document.id}", e)
                null
            }
        }
    }

    suspend fun updateLocation(location: Location): Result<Location> = kotlin.runCatching {
        val locationData = hashMapOf(
            "name" to location.name,
            "description" to location.description,
            "type" to location.type.name,
            "position" to hashMapOf(
                "latitude" to location.position.latitude,
                "longitude" to location.position.longitude
            ),
            "authorId" to location.authorId,
            "photos" to location.photos,
            "ratings" to location.ratings.map { rating ->
                hashMapOf(
                    "userId" to rating.userId,
                    "score" to rating.score,
                    "comment" to rating.comment,
                    "timestamp" to rating.timestamp
                )
            },
            "lastInteraction" to System.currentTimeMillis()
        )

        locationsCollection.document(location.id).update(locationData as Map<String, Any>).await()
        location.copy(lastInteraction = System.currentTimeMillis())
    }

    suspend fun deleteLocation(locationId: String): Result<Unit> = kotlin.runCatching {
        locationsCollection.document(locationId).delete().await()
    }
}
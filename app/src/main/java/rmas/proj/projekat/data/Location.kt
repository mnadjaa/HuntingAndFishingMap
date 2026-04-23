package rmas.proj.projekat.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import rmas.proj.projekat.data.Species
data class Location(
    @DocumentId var id: String = "",
    val name: String="",
    val description: String="",
    val type: LocationType=LocationType.HUNTING,//ako hocu both promeni da bude set
    val position: LatLng= LatLng(0.0, 0.0),//    var position: GeoPoint? = null,
    val photos: List<String> = emptyList(),
    val authorId: String="",
    val createdAt: Long = System.currentTimeMillis(),
    val lastInteraction: Long = System.currentTimeMillis(),
    val ratings: List<Rating> = emptyList(),
    val animalIds: List<String> = emptyList(),
    val fishIds: List<String> = emptyList()
)
class LocationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val locationsCollection = db.collection("locations")
    private val animalsCollection=db.collection("animals")
    private val fishCollection = db.collection("fishes")
    suspend fun createLocation(location: Location): Result<Location> = kotlin.runCatching {
        val locationDoc = locationsCollection.document()
        val timestamp = System.currentTimeMillis()
        val locationWithId = location.copy(
            id = locationDoc.id,
            photos = location.photos,
            createdAt =timestamp,// System.currentTimeMillis(),
            lastInteraction = timestamp//System.currentTimeMillis()
        )
        // konvertuje lokaciju u hashmap za firestore
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
            "animalIds" to locationWithId.animalIds,
            "fishIds" to locationWithId.fishIds,
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
                val positionMap = data["position"] as? Map<*, *>
                val position = if (positionMap != null) {
                    LatLng(
                        positionMap["latitude"] as Double,
                        positionMap["longitude"] as Double
                    )
                } else {
                    return@mapNotNull null
                }
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

                val animalIds = (data["animalIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val fishIds = (data["fishIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

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
                    lastInteraction = data["lastInteraction"] as? Long ?: System.currentTimeMillis(),
                    animalIds = animalIds,
                    fishIds = fishIds
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
            "lastInteraction" to System.currentTimeMillis(),
            "animalIds" to location.animalIds,
            "fishIds" to location.fishIds
        )
        locationsCollection.document(location.id).update(locationData as Map<String, Any>).await()
        location.copy(lastInteraction = System.currentTimeMillis())
    }
    suspend fun getLocation(locationId: String): Result<Location> = kotlin.runCatching {
        val document = locationsCollection.document(locationId).get().await()
        if (!document.exists()) {
            throw NoSuchElementException("Location with id $locationId not found")
        }
        val data = document.data ?: throw IllegalStateException("Location data is null")

        // position
        val positionMap = data["position"] as? Map<*, *>
            ?: throw IllegalStateException("Position data is missing")
        val position = LatLng(
            positionMap["latitude"] as Double,
            positionMap["longitude"] as Double
        )

        // ratings
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

        val animalIds = (data["animalIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val fishIds = (data["fishIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        Location(
            id = document.id,
            name = data["name"] as? String ?: throw IllegalStateException("Name is missing"),
            description = data["description"] as? String ?: throw IllegalStateException("Description is missing"),
            type = LocationType.valueOf(data["type"] as? String ?: throw IllegalStateException("Type is missing")),
            position = position,
            authorId = data["authorId"] as? String ?: throw IllegalStateException("AuthorId is missing"),
            photos = (data["photos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            ratings = ratings,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
            lastInteraction = data["lastInteraction"] as? Long ?: System.currentTimeMillis(),
            animalIds = animalIds,
            fishIds = fishIds
        )
    }
    suspend fun addSpeciesToLocation(
        locationId: String,
        speciesId: String,
        type: SpeciesType
    ): Result<Unit> {
        return try {
            db.runBatch { batch ->
                when (type) {
                    SpeciesType.ANIMAL -> {
                        batch.update(
                            locationsCollection.document(locationId),
                            "animalIds",
                            FieldValue.arrayUnion(speciesId)
                        )
                        batch.update(
                            animalsCollection.document(speciesId),
                            "locationIds",
                            FieldValue.arrayUnion(locationId)
                        )
                    }
                    SpeciesType.FISH -> {
                        batch.update(
                            locationsCollection.document(locationId),
                            "fishIds",
                            FieldValue.arrayUnion(speciesId)
                        )
                        batch.update(
                            fishCollection.document(speciesId),
                            "locationIds",
                            FieldValue.arrayUnion(locationId)
                        )
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSpeciesForLocation(
        locationId: String,
        type: SpeciesType
    ): Result<List<Species>> {
        return try {
            val snapshot = locationsCollection
                .document(locationId)
                .get()
                .await()

            val ids = when (type) {
                SpeciesType.ANIMAL ->
                    snapshot.get("animalIds") as? List<String> ?: emptyList()

                SpeciesType.FISH ->
                    snapshot.get("fishIds") as? List<String> ?: emptyList()
            }

            if (ids.isEmpty()) {
                return Result.success(emptyList())
            }

            val items: List<Species> = ids.chunked(10).flatMap { chunk ->
                when (type) {
                    SpeciesType.ANIMAL ->
                        animalsCollection
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()
                            .toObjects(Animal::class.java)
                    SpeciesType.FISH ->
                        fishCollection
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()
                            .toObjects(Fish::class.java)
                }
            }
            Result.success(items)
        }
        catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package rmas.proj.projekat.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class FishSighting(
    @DocumentId var id: String = "",
    val fishId: String = "",
    val locationId: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val reportedBy: String = ""  //userId ko je prijavio vidjanje
)

class FishSightingRepository{
    private val db = FirebaseFirestore.getInstance()
    private val sightingsCollection = db.collection("fish_sightings")

    // ako vec postoji sighting za tu kombinaciju updateuje se
    suspend fun updateLastSeen(fishId: String, locationId: String): Result<Unit> {
        return try {
            val query = sightingsCollection
                .whereEqualTo("fishId", fishId)
                .whereEqualTo("locationId", locationId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                // Vec postoji - samo update timestamp
                sightingsCollection.document(query.documents[0].id)
                    .update("lastSeen", System.currentTimeMillis())
                    .await()
            } else {
                // Novo vidjanje
                val sighting = FishSighting(
                    fishId = fishId,
                    locationId = locationId,
                    lastSeen = System.currentTimeMillis()
                )
                sightingsCollection.add(sighting).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Svi sightingsi za odredjenu zivotinju - na kojim lokacijama i kada
    suspend fun getSightingsForFish(fishId: String): Result<List<FishSighting>> {
        return try {
            val snapshot = sightingsCollection
                .whereEqualTo("fishId", fishId)
                .get().await()
            val sightings = snapshot.documents.mapNotNull { it.toObject(FishSighting::class.java) }
            Result.success(sightings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Sva vidjanja na odredjenoj lokaciji - koje životinje i kada
    suspend fun getSightingsForLocation(locationId: String): Result<List<FishSighting>> {
        return try {
            val snapshot = sightingsCollection
                .whereEqualTo("locationId", locationId)
                .get().await()
            val sightings = snapshot.documents.mapNotNull { it.toObject(FishSighting::class.java) }
            Result.success(sightings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // kada je određena životinja viđena na određenoj lokaciji
    suspend fun getLastSeen(fishId: String, locationId: String): Result<FishSighting?> {
        return try {
            val snapshot = sightingsCollection
                .whereEqualTo("fishId", fishId)
                .whereEqualTo("locationId", locationId)
                .get().await()
            val sighting = snapshot.documents.firstOrNull()?.toObject(FishSighting::class.java)
            Result.success(sighting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

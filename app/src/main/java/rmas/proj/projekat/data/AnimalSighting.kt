package rmas.proj.projekat.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AnimalSighting(
    @DocumentId var id: String = "",
    val animalId: String = "",
    val locationId: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val reportedBy: String = ""  //userId ko je prijavio
)

class AnimalSightingRepository{
    private val db = FirebaseFirestore.getInstance()
    private val sightingsCollection = db.collection("animal_sightings")

    // ako već postoji sighting za tu kombinaciju update-uj timestamp
    suspend fun updateLastSeen(animalId: String, locationId: String): Result<Unit> {
        return try {
            val query = sightingsCollection
                .whereEqualTo("animalId", animalId)
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
                val sighting = AnimalSighting(
                    animalId = animalId,
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

    // Sva vidjanja za odredjenu zivotinju - na kojim lokacijama i kada
    suspend fun getSightingsForAnimal(animalId: String): Result<List<AnimalSighting>> {
        return try {
            val snapshot = sightingsCollection
                .whereEqualTo("animalId", animalId)
                .get().await()
            val sightings = snapshot.documents.mapNotNull { it.toObject(AnimalSighting::class.java) }
            Result.success(sightings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sva vidjanja na odredjenoj lokaciji - koje zivotinje i kada)
    suspend fun getSightingsForLocation(locationId: String): Result<List<AnimalSighting>> {
        return try {
            val snapshot = sightingsCollection
                .whereEqualTo("locationId", locationId)
                .get().await()
            val sightings = snapshot.documents.mapNotNull { it.toObject(AnimalSighting::class.java) }
            Result.success(sightings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // kada je odredjena zivotinja vidjena na odredjenoj lokaciji
    suspend fun getLastSeen(animalId: String, locationId: String): Result<AnimalSighting?> {
        return try {
            val snapshot = sightingsCollection
                .whereEqualTo("animalId", animalId)
                .whereEqualTo("locationId", locationId)
                .get().await()
            val sighting = snapshot.documents.firstOrNull()?.toObject(AnimalSighting::class.java)
            Result.success(sighting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

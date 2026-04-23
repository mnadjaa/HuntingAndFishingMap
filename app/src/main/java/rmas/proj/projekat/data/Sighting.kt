package rmas.proj.projekat.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class Sighting(
    @DocumentId var id: String = "",
    val speciesId: String = "", //id zivotinje ili ribe
    val locationId: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val reportedBy: String = ""  //userId ko je prijavio
)

class SightingRepository{
    private val db = FirebaseFirestore.getInstance()
    private val sightingsCollection = db.collection("species_sightings")

    // ako vec postoji sighting za tu kombinaciju update-uj timestamp
    suspend fun updateLastSeen(
        speciesId: String,
        locationId: String,
        reportedBy: String = ""
    ): Result<Unit> {
        return try {
            val query = sightingsCollection
                .whereEqualTo("speciesId", speciesId)
                .whereEqualTo("locationId", locationId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                sightingsCollection.document(query.documents[0].id)
                    .update("lastSeen", System.currentTimeMillis())
                    .await()
            }
            else {
                val sighting = Sighting(
                    speciesId = speciesId,
                    locationId = locationId,
                    lastSeen = System.currentTimeMillis(),
                    reportedBy = reportedBy
                )
                sightingsCollection.add(sighting).await()
            }
            Result.success(Unit)
        }
        catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Sva vidjenja na odredjenoj lokaciji - koje zivotinje/ribe i kada)
    suspend fun getSightingsForLocation(
        locationId: String
    ): Result<List<Sighting>> {
        return try {
            val snapshot = sightingsCollection
                .whereEqualTo("locationId", locationId)
                .get()
                .await()

            val sightings = snapshot.documents
                .mapNotNull { it.toObject(Sighting::class.java) }

            Result.success(sightings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSightingsFlow(): Flow<List<Sighting>> = callbackFlow {
        val db = FirebaseFirestore.getInstance()

        val listener = db.collection("species_sightings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sightings = snapshot.documents
                        .mapNotNull { it.toObject(Sighting::class.java) }
                    trySend(sightings)
                }
            }

        awaitClose { listener.remove() }
    }
}


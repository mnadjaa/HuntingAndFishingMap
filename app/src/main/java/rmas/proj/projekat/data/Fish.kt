package rmas.proj.projekat.data

import android.net.Uri
import com.google.firebase.firestore.DocumentId
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import rmas.proj.projekat.utils.uploadImageToCloudinary

data class Fish(
    @DocumentId var id: String = "",          // Ovaj ID je automatski dodeljen od strane Firestore-a
    val name: String = "",
    val description: String = "",
    val photo: String = "",  // val photos: List<String> = emptyList<String>(),
    val authorId: String = "",//OVO IZBACI POSTO NECE KORISNICI MOCI DA DODAJU ZIVOTINJE 🐛
    val locationIds: List<String> = emptyList()
)

class FishRepository {
    private val db = FirebaseFirestore.getInstance()
    private val fishesCollection = db.collection("fishes")

    private val _fishes = MutableStateFlow<Map<String, Fish>>(emptyMap())
    val fishes: Flow<Map<String, Fish>> = _fishes.asStateFlow()

    suspend fun getFishById(fishId: String): Result<Fish> {
        return try {
            Log.d(TAG, "Fetching fish with ID: $fishId")
            val document = fishesCollection.document(fishId).get().await()

            if (document.exists()) {
                val fish = document.toObject(Fish::class.java)
                if (fish != null) {
                    _fishes.value = _fishes.value + (fishId to fish)
                    Log.d(TAG, "Successfully fetched fish: ${fish.name}")
                    Result.success(fish)
                } else {
                    Log.w(TAG, "Failed to convert document to Fish object")
                    Result.failure(IllegalStateException("Failed to convert document to Fish object"))
                }
            } else {
                Log.w(TAG, "Fish document does not exist")
                Result.failure(NoSuchElementException("Fish not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching fish", e)
            Result.failure(e)
        }
    }

    suspend fun getFishesByAuthor(authorId: String): Result<List<Fish>> {
        return try {
            val snapshot = fishesCollection.whereEqualTo("authorId", authorId).get().await()
            val fishes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Fish::class.java)
            }

            // Update internal state
            val fishMap = fishes.associateBy { it.id }
            _fishes.value = _fishes.value + fishMap

            Log.d(TAG, "Successfully fetched ${fishes.size} fishes by author $authorId")
            Result.success(fishes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching fishes by author", e)
            Result.failure(e)
        }
    }

    suspend fun getAllFishes(): Result<List<Fish>> {
        return try {
            val snapshot = fishesCollection.get().await()
            val fishes = snapshot.documents.mapNotNull { doc ->
                val fish = doc.toObject(Fish::class.java)
                Log.d("FishRepository", "Fetched fish: ${fish?.name}, photo: ${fish?.photo}")
                fish
            }

            // Update internal state
            val fishMap = fishes.associateBy { it.id }
            _fishes.value = fishMap

            Log.d(TAG, "Successfully fetched all ${fishes.size} fishes")
            Result.success(fishes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all fishes", e)
            Result.failure(e)
        }
    }

    suspend fun createFish(fish: Fish, context: android.content.Context, imageUri: Uri?): Result<Fish> {
        return try {
            val documentRef = fishesCollection.document()
            val fishWithId = fish.copy(id = documentRef.id)

            var imageUrl: String? = null
            if (imageUri != null) {
                imageUrl = uploadImageToCloudinary(context, imageUri)
            }

            // Use the cloudinary URL instead of local URI
            val finalFish = fishWithId.copy(photo = imageUrl ?: "")
            documentRef.set(finalFish).await()

            // Update the internal state
            _fishes.value = _fishes.value + (finalFish.id to finalFish)
            Log.d(TAG, "Successfully created fish with ID: ${finalFish.id}")

            Result.success(finalFish)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating fish", e)
            Result.failure(e)
        }
    }

    suspend fun updateFish(fish: Fish): Result<Fish> {
        return try {
            fishesCollection.document(fish.id).set(fish).await()

            // Update the internal state
            _fishes.value = _fishes.value + (fish.id to fish)
            Log.d(TAG, "Successfully updated fish with ID: ${fish.id}")

            Result.success(fish)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating fish", e)
            Result.failure(e)
        }
    }

    suspend fun deleteFish(fishId: String): Result<Unit> {
        return try {
            fishesCollection.document(fishId).delete().await()

            // Update the internal state
            _fishes.value = _fishes.value - fishId
            Log.d(TAG, "Successfully deleted fish with ID: $fishId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting fish", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FishRepository"
    }
}
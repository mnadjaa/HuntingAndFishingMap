package rmas.proj.projekat.data

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.DocumentId
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import rmas.proj.projekat.uploadImageToCloudinary

data class Fish(
    @DocumentId var id: String = "",          // Ovaj ID je automatski dodeljen od strane Firestore-a
    val name: String = "",
    val description: String = "",
    val photo: String = "",  // val photos: List<String> = emptyList<String>(),
    val authorId: String = ""
)


class FishViewModel(
    private val fishRepository: FishRepository = FishRepository()
) : ViewModel() {

    private val _fishData = MutableStateFlow<Map<String, Fish>>(emptyMap())
    val fishData: StateFlow<Map<String, Fish>> = _fishData.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun loadFishData(fishId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.getFishById(fishId)
                .onSuccess { fish ->
                    _fishData.value = _fishData.value + (fishId to fish)
                    Log.d("FishViewModel", "Uspešno učitana riba: ${fish.name}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju ribe: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri učitavanju ribe", exception)
                }

            _isLoading.value = false
        }
    }

    fun loadFishesByAuthor(authorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.getFishesByAuthor(authorId)
                .onSuccess { fishes ->
                    val fishMap = fishes.associateBy { it.id }
                    _fishData.value = _fishData.value + fishMap
                    Log.d("FishViewModel", "Uspešno učitane ribe autora: ${fishes.size}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju riba: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri učitavanju riba", exception)
                }

            _isLoading.value = false
        }
    }

    fun loadAllFishes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.getAllFishes()
                .onSuccess { fishes ->
                    val fishMap = fishes.associateBy { it.id }
                    _fishData.value = fishMap
                    Log.d("FishViewModel", "Uspešno učitane sve ribe: ${fishes.size}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju riba: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri učitavanju riba", exception)
                }

            _isLoading.value = false
        }
    }

    fun createFish(fish: Fish, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.createFish(fish, context, _selectedImageUri.value)
                .onSuccess { createdFish ->
                    _fishData.value = _fishData.value + (createdFish.id to createdFish)
                    Log.d("FishViewModel", "Uspešno kreirana riba: ${createdFish.id}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri kreiranju ribe: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri kreiranju ribe", exception)
                }

            _isLoading.value = false
        }
    }

    fun updateFish(fish: Fish) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.updateFish(fish)
                .onSuccess { updatedFish ->
                    _fishData.value = _fishData.value + (updatedFish.id to updatedFish)
                    Log.d("FishViewModel", "Uspešno ažurirana riba: ${updatedFish.id}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri ažuriranju ribe: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri ažuriranju ribe", exception)
                }

            _isLoading.value = false
        }
    }

    fun deleteFish(fishId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.deleteFish(fishId)
                .onSuccess {
                    _fishData.value = _fishData.value - fishId
                    Log.d("FishViewModel", "Uspešno obrisana riba: $fishId")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri brisanju ribe: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri brisanju ribe", exception)
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun selectImage(context: Context) {
        val pickImageLauncher =
            (context as? ComponentActivity)?.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { updateSelectedImage(it) }
            }
        pickImageLauncher?.launch("image/*")
    }
}

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
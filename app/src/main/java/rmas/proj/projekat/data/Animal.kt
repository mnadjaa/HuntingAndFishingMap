package rmas.proj.projekat.data

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import rmas.proj.projekat.utils.uploadImageToCloudinary

data class Animal(
    @DocumentId var id: String = "",          // Ovaj ID je automatski dodeljen od strane Firestore-a
    val name: String = "",
    val description: String = "",
    val photo: String = "",  // val photos: List<String> = emptyList<String>(),
    val authorId: String = "",//OVO IZBACI POSTO NECE KORISNICI MOCI DA DODAJU ZIVOTINJE 🐛
    val locationIds: List<String> = emptyList()
)

class AnimalRepository {
    private val db = FirebaseFirestore.getInstance()
    private val animalsCollection = db.collection("animals")

    private val _animals = MutableStateFlow<Map<String, Animal>>(emptyMap())
    val animals: Flow<Map<String, Animal>> = _animals.asStateFlow()

    suspend fun getAnimalById(animalId: String): Result<Animal> {
        return try {
            Log.d(TAG, "Fetching animal with ID: $animalId")
            val document = animalsCollection.document(animalId).get().await()

            if (document.exists()) {
                val animal = document.toObject(Animal::class.java)
                if (animal != null) {
                    _animals.value = _animals.value + (animalId to animal)
                    Log.d(TAG, "Successfully fetched animal: ${animal.name}")
                    Result.success(animal)
                } else {
                    Log.w(TAG, "Failed to convert document to Animal object")
                    Result.failure(IllegalStateException("Failed to convert document to Animal object"))
                }
            } else {
                Log.w(TAG, "Animal document does not exist")
                Result.failure(NoSuchElementException("Animal not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching animal", e)
            Result.failure(e)
        }
    }

    suspend fun getAnimalsByAuthor(authorId: String): Result<List<Animal>> {
        return try {
            val snapshot = animalsCollection.whereEqualTo("authorId", authorId).get().await()
            val animals = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Animal::class.java)
            }

            // Update internal state
            val animalMap = animals.associateBy { it.id }
            _animals.value = _animals.value + animalMap

            Log.d(TAG, "Successfully fetched ${animals.size} animals by author $authorId")
            Result.success(animals)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching animals by author", e)
            Result.failure(e)
        }
    }

    suspend fun getAllAnimals(): Result<List<Animal>> {
        return try {
            val snapshot = animalsCollection.get().await()
            val animals = snapshot.documents.mapNotNull { doc ->
                //doc.toObject(Animal::class.java)
                val animal = doc.toObject(Animal::class.java)
                Log.d("AnimalRepository", "Fetched animal: ${animal?.name}, photo: ${animal?.photo}")
                animal
            }

            // Update internal state
            val animalMap = animals.associateBy { it.id }
            _animals.value = animalMap

            Log.d(TAG, "Successfully fetched all ${animals.size} animals")
            Result.success(animals)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all animals", e)
            Result.failure(e)
        }
    }

    suspend fun createAnimal(animal: Animal, context: android.content.Context, imageUri: Uri?): Result<Animal> {
        return try {
            val documentRef = animalsCollection.document()
            val animalWithId = animal.copy(id = documentRef.id)

            var imageUrl: String? = null
            if (imageUri != null) {
                imageUrl = uploadImageToCloudinary(context, imageUri)
            }

            // Use the cloudinary URL instead of local URI
            val finalAnimal = animalWithId.copy(photo = imageUrl?:"")
            documentRef.set(finalAnimal).await()

            // Update the internal state
            _animals.value = _animals.value + (finalAnimal.id to finalAnimal)
            Log.d(TAG, "Successfully created animal with ID: ${finalAnimal.id}")

            Result.success(finalAnimal)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating animal", e)
            Result.failure(e)
        }
    }

    suspend fun updateAnimal(animal: Animal): Result<Animal> {
        return try {
            animalsCollection.document(animal.id).set(animal).await()

            // Update the internal state
            _animals.value = _animals.value + (animal.id to animal)
            Log.d(TAG, "Successfully updated animal with ID: ${animal.id}")

            Result.success(animal)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating animal", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAnimal(animalId: String): Result<Unit> {
        return try {
            animalsCollection.document(animalId).delete().await()

            // Update the internal state
            _animals.value = _animals.value - animalId
            Log.d(TAG, "Successfully deleted animal with ID: $animalId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting animal", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AnimalRepository"
    }
}
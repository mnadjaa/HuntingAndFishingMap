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
    @DocumentId override var id: String = "",          // Ovaj ID je automatski dodeljen od strane Firestore-a
    override val name: String = "",
    override val description: String = "",
    override val image: String = "",  // val photos: List<String> = emptyList<String>(), DA PROMENIM U IMAGE?
    override val authorId: String = "",//OVO IZBACI POSTO NECE KORISNICI MOCI DA DODAJU ZIVOTINJE 🐛
    override val locationIds: List<String> = emptyList()
): Species

class AnimalRepository {
    private val repo = SpeciesRepository(
        collectionName = "animals",
        clazz = Animal::class.java //firestore metoda ocekuje java tip pa mora da se konvertuje
    )
    suspend fun getAllAnimals(): Result<List<Animal>> {
        return repo.getAll()
    }
}

package rmas.proj.projekat.data

import com.google.firebase.firestore.DocumentId

data class Animal(
    @DocumentId override var id: String = "",          // id je automatski dodeljen od strane firestorea
    override val name: String = "",
    override val description: String = "",
    override val image: String = "",
    override val authorId: String = "",//ovo mi ipak ne treba jer ne dodaju ih korisnici
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

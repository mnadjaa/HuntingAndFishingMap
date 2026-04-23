package rmas.proj.projekat.data

import com.google.firebase.firestore.DocumentId

data class Fish(
    @DocumentId override var id: String = "",
    override val name: String = "",
    override val description: String = "",
    override val image: String = "",  // val photos: List<String> = emptyList<String>(),
    override val authorId: String = "",//x
    override val locationIds: List<String> = emptyList()
): Species

class FishRepository {
    private val repo = SpeciesRepository(
        collectionName = "fishes",
        clazz = Fish::class.java
    )
    suspend fun getAllFishes(): Result<List<Fish>> {
        return repo.getAll()
    }
}
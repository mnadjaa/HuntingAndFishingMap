package rmas.proj.projekat.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface Species {
    val id: String
    val name: String
    val description: String
    val image: String
    val authorId: String
    val locationIds: List<String>
}

class SpeciesRepository<T : Species>(
    private val collectionName: String,
    private val clazz: Class<T>
) {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection(collectionName)
    suspend fun getAll(): Result<List<T>> {
        return try {
            val snapshot = collection.get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                val item = doc.toObject(clazz)
                item
            }
            Log.d("SpeciesRepository", "Fetched ${items.size} items from $collectionName")
            Result.success(items)
        } catch (e: Exception) {
            Log.e("SpeciesRepository", "Error fetching $collectionName", e)
            Result.failure(e)
        }
    }
}
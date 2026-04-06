package rmas.proj.projekat.data

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class User(
    @DocumentId var id: String = "",          // Ovaj ID je automatski dodeljen od strane Firestore-a
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val profilePicture: String = ""          // URL slike korisnika (sačuvane u cloudinary)
)
class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    private val _users = MutableStateFlow<Map<String, User>>(emptyMap())
    val users: Flow<Map<String, User>> = _users.asStateFlow()

    suspend fun getUserById(userId: String): Result<User> {
        return try {
            Log.d(TAG, "Fetching user with ID: $userId")
            val document = usersCollection.document(userId).get().await()

            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    // Update the internal state
                    _users.value = _users.value + (userId to user)
                    Log.d(TAG, "Successfully fetched user: ${user.firstName} ${user.lastName}")
                    Result.success(user)
                } else {
                    Log.w(TAG, "Failed to convert document to User object")
                    Result.failure(IllegalStateException("Failed to convert document to User object"))
                }
            } else {
                Log.w(TAG, "User document does not exist")
                Result.failure(NoSuchElementException("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user", e)
            Result.failure(e)
        }
    }
    suspend fun createUser(user: User): Result<User> {
        return try {
            val documentRef = usersCollection.document()
            val userWithId = user.copy(id = documentRef.id)
            documentRef.set(userWithId).await()

            // Update the internal state
            _users.value = _users.value + (userWithId.id to userWithId)
            Log.d(TAG, "Successfully created user with ID: ${userWithId.id}")

            Result.success(userWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            Result.failure(e)
        }
    }
    suspend fun updateUser(user: User): Result<User> {
        return try {
            usersCollection.document(user.id).set(user).await()

            // Update the internal state
            _users.value = _users.value + (user.id to user)
            Log.d(TAG, "Successfully updated user with ID: ${user.id}")

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()

            // Update the internal state
            _users.value = _users.value - userId
            Log.d(TAG, "Successfully deleted user with ID: $userId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            Result.failure(e)
        }
    }
    companion object {
        private const val TAG = "UserRepository"
    }
}

package rmas.proj.projekat.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import rmas.proj.projekat.data.User
import java.io.File

suspend fun uploadImageAndSaveUser(
    context: Context,
    userId: String,
    imageUri: Uri,
    firstName: String,
    lastName: String,
    phoneNumber: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "temp_image.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val cloudinary = Cloudinary(
                ObjectUtils.asMap(
                    "cloud_name", "dsoyvcupj",
                    "api_key", "766928287421829",
                    "api_secret", "DVFDtQ6PRj3K4cJLsaej8Z-_IRw"
                )
            )

            val result = cloudinary.uploader().upload(file, ObjectUtils.emptyMap())
            val imageUrl = result["secure_url"] as String

            val user = User(
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                profilePicture = imageUrl
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(user)
                .await()

            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }
}

suspend fun uploadImageToCloudinary(context: Context, imageUri: Uri): String? {//suspend - nije blokirajuca
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "temp_image.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }

            val cloudinary = Cloudinary(
                ObjectUtils.asMap(
                    "cloud_name", "dsoyvcupj",
                    "api_key", "766928287421829",
                    "api_secret", "DVFDtQ6PRj3K4cJLsaej8Z-_IRw"
                )
            )

            val result = cloudinary.uploader().upload(file, ObjectUtils.emptyMap())
            result["secure_url"] as? String
        } catch (e: Exception) {
            Log.e( "AnimalRepository","Error uploading image to Cloudinary", e)
            null
        }
    }
}


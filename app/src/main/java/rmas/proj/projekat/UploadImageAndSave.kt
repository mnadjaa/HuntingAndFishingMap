package rmas.proj.projekat

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun uploadImageToCloudinary(context: Context, imageUri: Uri): String? {
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
package rmas.proj.projekat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.firestore.FirebaseFirestore
import rmas.proj.projekat.data.User
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import java.io.File
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.ViewModel
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

class RegisterActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    /*private val cloudinary = Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", "dsoyvcupj",
            "api_key", "766928287421829",
            "api_secret", "DVFDtQ6PRj3K4cJLsaej8Z-_IRw"
        )
    )*/

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            viewModel.updateSelectedImage(imageUri)
        }
    }

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Projekat_NoActionBar)
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContent {
            val selectedImageUri by viewModel.selectedImageUri.collectAsState()
            RegisterScreen(
                navController = rememberNavController(),//null,
                selectedImageUri = selectedImageUri,
                onNavigateToLogin = {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                },
                onRegisterSuccess = {
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                },
                onImageSelect = {
                    pickImageLauncher.launch("image/*")
                }
            )
        }
    }
}

class RegisterViewModel : ViewModel() {
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri = _selectedImageUri.asStateFlow()

    fun updateSelectedImage(uri: Uri) {
        _selectedImageUri.value = uri
    }
}

@Composable
fun RegisterScreen(
    navController: NavController,
    selectedImageUri: Uri?,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onImageSelect: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image Selection
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onImageSelect() }
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Profilna slika",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Dodaj Profilnu sliku",
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                )
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Korisničko ime (Email)", color = Color.White) },
            isError = isError && username.isEmpty(),
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Lozinka", color = Color.White) },
            visualTransformation = PasswordVisualTransformation(),
            isError = isError && password.isEmpty(),
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Ime", color = Color.White) },
            isError = isError && firstName.isEmpty(),
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Prezime", color = Color.White) },
            isError = isError && lastName.isEmpty(),
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Telefon", color = Color.White) },
            isError = isError && phoneNumber.isEmpty(),
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() ||
                    lastName.isEmpty() || phoneNumber.isEmpty() || selectedImageUri == null) {
                    isError = true
                    Toast.makeText(context, "Popunite sva polja i dodajte profilnu sliku", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                auth.createUserWithEmailAndPassword(username, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                scope.launch {
                                    uploadImageAndSaveUser(
                                        context = context,
                                        userId = user.uid,
                                        imageUri = selectedImageUri!!,
                                        firstName = firstName,
                                        lastName = lastName,
                                        phoneNumber = phoneNumber,
                                        onSuccess = {
                                            Toast.makeText(context, "Registracija Uspešno izvršena", Toast.LENGTH_SHORT).show()
                                            onRegisterSuccess()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Registration Failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registruj se")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Već imate nalog? Prijavite se")
        }
    }
}

private suspend fun uploadImageAndSaveUser(
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

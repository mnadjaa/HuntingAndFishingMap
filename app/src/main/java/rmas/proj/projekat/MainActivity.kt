    package rmas.proj.projekat

    import androidx.lifecycle.ViewModel
    import java.io.File
    import android.content.Intent
    import android.net.Uri
    import android.os.Bundle
    import android.widget.Toast
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.activity.viewModels
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import com.google.firebase.auth.FirebaseAuth
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Person
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.text.input.PasswordVisualTransformation
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.layout.ContentScale
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavController
    import androidx.navigation.compose.rememberNavController
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.input.KeyboardType
    import coil.compose.rememberImagePainter
    import com.google.firebase.FirebaseApp
    import kotlinx.coroutines.launch
    import rmas.proj.projekat.data.UserViewModel
    import androidx.navigation.compose.*
    import coil.compose.AsyncImage
    import com.google.firebase.firestore.FirebaseFirestore
    import android.content.Context
    import androidx.compose.foundation.rememberScrollState
    import com.cloudinary.Cloudinary
    import com.cloudinary.utils.ObjectUtils
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.tasks.await
    import kotlinx.coroutines.withContext
    import rmas.proj.projekat.data.User

    class MainActivity : ComponentActivity() {
        private lateinit var auth: FirebaseAuth
        private val registerViewModel: RegisterViewModel by viewModels()//cuva stanje vezano za registraciju
        private val userViewModel: UserViewModel by viewModels()//dohvata ulogovanog korisnika sa firestorea
        private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> //activity resault launcher za izbor slika iz galerije
            uri?.let { imageUri ->
                registerViewModel.updateSelectedImage(imageUri)
            }
        }
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            auth = FirebaseAuth.getInstance()
            FirebaseApp.initializeApp(this)
            val user = FirebaseAuth.getInstance().currentUser

            setContent {//compose fja koja prikazuje ui
                val navController = rememberNavController()
                val isLogged = auth.currentUser != null

                AppNavigation(
                    registerViewModel = registerViewModel,
                    userViewModel = userViewModel,
                    onImageSelect = {
                        pickImageLauncher.launch("image/*")
                    }
                )
            }
        }
    }

    @Composable
    fun LoginScreen(
        navController: NavController,
        onNavigateToRegister: () -> Unit,
        onLoginSuccess: () -> Unit
    ) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        val auth = FirebaseAuth.getInstance()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()//za asinhrone operacije - zbog firebase poziva

        val userViewModel: UserViewModel = viewModel()
        val userState by userViewModel.usersData.collectAsState() // Prati promene u korisničkim podacima
        val usersData by userViewModel.usersData.collectAsState()

        // Prati podatke trenutno prijavljenog korisnika
        val currentUserId = auth.currentUser?.uid
        val currentUser = currentUserId?.let { usersData[it] }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Korisničko ime (Email)") },
                isError = isError && username.isEmpty(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Lozinka") },
                visualTransformation = PasswordVisualTransformation(),
                isError = isError && password.isEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            if (isError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (username.isEmpty() || password.isEmpty()) {
                        isError = true
                        errorMessage = "Popunite sva polja"
                        return@Button
                    }

                    scope.launch {
                        try {
                            auth.signInWithEmailAndPassword(username, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(
                                            context,
                                            "Uspešna prijava",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onLoginSuccess()
                                    } else {
                                        isError = true
                                        errorMessage = "Pogrešno korisničko ime ili lozinka"
                                        Toast.makeText(
                                            context,
                                            "Greška pri prijavi: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } catch (e: Exception) {
                            isError = true
                            errorMessage = e.message ?: "Greška pri prijavi"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prijava")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { onNavigateToRegister() }) {
                Text("Registracija (Kreiraj novi nalog)")
            }
        }
    }


    //dodato iz home activity    🤸🤸🤸🤸
    // Ovo je bilo u HomeActivity kao AppNavigation
    @Composable
    fun AppNavigation(
        registerViewModel: RegisterViewModel,
        userViewModel: UserViewModel,
        onImageSelect: () -> Unit
    ) {
        val navController = rememberNavController()
        val selectedImageUri by registerViewModel.selectedImageUri.collectAsState()
        val startDestination = "login"

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("login") {
                LoginScreen(
                    navController = navController,
                    onNavigateToRegister = {
                        navController.navigate("register")
                    },
                    onLoginSuccess = {
                        navController.navigate("map") {//main
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    navController = navController,
                    selectedImageUri = selectedImageUri,
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onRegisterSuccess = {
                        navController.navigate("main") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onImageSelect = onImageSelect
                )
            }

            composable("main") {
                MainScreen(navController = navController)
            }

            composable("map") {
                MapScreen(navController = navController)
            }
            composable("profile") {
                UserProfileScreen()
            }

            composable("users") {
                UsersScreen()
            }

            composable("animals") {
                AnimalsScreen()
            }

            composable("fishes") {
                FishesScreen()
            }

            composable("service_control") {
                val context = LocalContext.current
                ServiceControlScreen(context = context)
            }
        }
    }


    // ==================== REGISTER SCREEN ====================
// Ovo je bilo u RegisterActivity - ostaje ISTO
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
                label = { Text("Korisničko ime (Email)") },
                isError = isError && username.isEmpty(),
                //textStyle = TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Lozinka") },
                visualTransformation = PasswordVisualTransformation(),
                isError = isError && password.isEmpty(),
                //textStyle = TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Ime") },
                isError = isError && firstName.isEmpty(),
                //textStyle = TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Prezime") },
                isError = isError && lastName.isEmpty(),
                //textStyle = TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Telefon") },
                isError = isError && phoneNumber.isEmpty(),
                //textStyle = TextStyle(color = Color.White),
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

    // ==================== REGISTER VIEW MODEL ====================
// Bilo u RegisterActivity - sada ovde
    class RegisterViewModel : ViewModel() {
        private val _selectedImageUri = MutableStateFlow<Uri?>(null)
        val selectedImageUri = _selectedImageUri.asStateFlow()

        fun updateSelectedImage(uri: Uri) {
            _selectedImageUri.value = uri
        }
    }

    // ==================== HELPER FUNCTION ====================
// Bilo u RegisterActivity - sada ovde
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


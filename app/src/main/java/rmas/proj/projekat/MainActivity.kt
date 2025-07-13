    package rmas.proj.projekat

    import android.content.Intent
    import android.net.Uri
    import android.os.Bundle
    import android.widget.Toast
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.activity.viewModels
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import com.google.firebase.auth.FirebaseAuth
    import androidx.compose.foundation.layout.*
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.text.input.PasswordVisualTransformation
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.graphics.Color
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavController
    import androidx.navigation.compose.rememberNavController
    import androidx.compose.ui.platform.LocalContext
    import coil.compose.rememberImagePainter
    import com.google.firebase.FirebaseApp
    import kotlinx.coroutines.launch
    import rmas.proj.projekat.data.UserViewModel


    class MainActivity : ComponentActivity() {
        private lateinit var auth: FirebaseAuth
        private val registerViewModel: RegisterViewModel by viewModels()
        private val userViewModel: UserViewModel by viewModels()
        private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { imageUri ->
                registerViewModel.updateSelectedImage(imageUri)
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            auth = FirebaseAuth.getInstance()

            FirebaseApp.initializeApp(this)


            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {  // Ako korisnik vec postoji, saljemo ga u HomeActivity
                startActivity(Intent(this, HomeActivity::class.java))
                finish() // Zatvaramo LoginActivity da se ne moze vratiti nazad pritiskom na Back dugme
            }


            setContent {
                val navController = rememberNavController()
                AppNavigation(
                    registerViewModel = registerViewModel,
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
        val scope = rememberCoroutineScope()//???

        val userViewModel: UserViewModel = viewModel() // Dodaj UserViewModel
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




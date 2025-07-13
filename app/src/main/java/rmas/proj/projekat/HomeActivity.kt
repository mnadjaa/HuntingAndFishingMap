package rmas.proj.projekat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState

class HomeActivity : ComponentActivity() {
    private val registerViewModel: RegisterViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            registerViewModel.updateSelectedImage(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation(
                registerViewModel = registerViewModel,
                onImageSelect = {
                    pickImageLauncher.launch("image/*")
                }
            )
        }
    }

    //ovo mi ne treba odjavljuje se i bez - svakako ne brisi neka ga
    /*override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().signOut()  // Automatsko odjavljivanje korisnika
    }*/
}

@Composable
fun AppNavigation(
    registerViewModel: RegisterViewModel,
    onImageSelect: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                navController = navController,
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("map") {
            MapScreen()
        }
        composable("main") {
            MainScreen()
        }
        composable("register") {
            val selectedImageUri = registerViewModel.selectedImageUri.collectAsState()

            RegisterScreen(
                navController = navController,
                selectedImageUri = selectedImageUri.value,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
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
    }
}
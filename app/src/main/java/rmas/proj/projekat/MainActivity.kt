    package rmas.proj.projekat

    import android.net.Uri
    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.activity.viewModels
    import androidx.compose.foundation.layout.Row
    import androidx.compose.material3.NavigationBar
    import androidx.compose.material3.NavigationBarItem
    import androidx.compose.material3.Scaffold
    import androidx.compose.runtime.Composable
    import com.google.firebase.auth.FirebaseAuth
    import androidx.compose.runtime.*
    import androidx.navigation.compose.rememberNavController
    import androidx.compose.ui.platform.LocalContext
    import androidx.navigation.NavHostController
    import com.google.firebase.FirebaseApp
    import rmas.proj.projekat.viewmodel.UserViewModel
    import androidx.navigation.compose.*
    import rmas.proj.projekat.screens.LoginScreen
    import rmas.proj.projekat.screens.RegisterScreen
    import rmas.proj.projekat.viewmodel.RegisterViewModel
    import rmas.proj.projekat.screens.AnimalsScreen
    import rmas.proj.projekat.screens.UserProfileScreen
    import rmas.proj.projekat.screens.UsersScreen
    import rmas.proj.projekat.screens.FishesScreen
    import rmas.proj.projekat.screens.MapScreen
    import rmas.proj.projekat.screens.ServiceControlScreen
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.People
    import androidx.compose.material.icons.filled.Person
    import androidx.compose.material.icons.filled.Search
    import androidx.compose.material.icons.filled.Settings
    import androidx.compose.material3.Icon
    import androidx.compose.material3.Text
    import androidx.compose.ui.Modifier
    import androidx.compose.runtime.collectAsState
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material.icons.filled.Map
    import androidx.lifecycle.viewmodel.compose.viewModel
    import rmas.proj.projekat.screens.SearchScreen
    import rmas.proj.projekat.viewmodel.AnimalViewModel
    import rmas.proj.projekat.viewmodel.FishViewModel
    import rmas.proj.projekat.viewmodel.LocationViewModel

    //NIJE MI VALJDA DOBRO TO STO SE PAMTI CELA ISTORIJA EKRANA VALJDA NE TREBA? MOZDA I TREBA PROVERI 🐛
    class MainActivity : ComponentActivity() {
        private lateinit var auth: FirebaseAuth
        private val registerViewModel: RegisterViewModel by viewModels()//cuva stanje vezano za registraciju
        private val locationViewModel: LocationViewModel by viewModels()
        private val animalViewModel: AnimalViewModel by viewModels()
        private val fishViewModel: FishViewModel by viewModels()
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
                    locationViewModel = locationViewModel,
                    animalViewModel= animalViewModel,
                    fishViewModel=fishViewModel,
                    onImageSelect = {
                        pickImageLauncher.launch("image/*")
                    }
                )
            }
        }
    }
    @Composable
    fun AppNavigation(
        registerViewModel: RegisterViewModel,
        locationViewModel: LocationViewModel,
        animalViewModel: AnimalViewModel,
        fishViewModel:FishViewModel,
        onImageSelect: () -> Unit
    ) {
        val navController = rememberNavController()
        val selectedImageUri by registerViewModel.selectedImageUri.collectAsState()
        val startDestination = "login"

        //val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
        DisposableEffect(Unit) {
            val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                isLoggedIn = auth.currentUser != null
            }
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

            onDispose {
                FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
            }
        }

        if (!isLoggedIn) { // ako nije ulogovan, prikazi login/register bez navbara
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
                            isLoggedIn=true
                            //navController.navigate("map") {//main
                                //popUpTo("login") { inclusive = true }
                            //}
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
                                //popUpTo("register") { inclusive = true }
                                isLoggedIn = true
                            }
                        },
                        onImageSelect = onImageSelect
                    )
                }
            }
        }
        else{//ako je ulogovan pokazuje navbar
            MainScreenWithNavigation(
                navController = navController,
                locationViewModel = locationViewModel,
                animalViewModel=animalViewModel,
                fishViewModel=fishViewModel,
                onLogout = { isLoggedIn = false }  // Callback za logout
            )
        }
    }
    @Composable
    fun MainScreenWithNavigation(
        navController: NavHostController,
        locationViewModel: LocationViewModel,
        animalViewModel:AnimalViewModel,
        fishViewModel: FishViewModel,
        onLogout: () -> Unit
        ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Pretraga") },
                        label = { Text("Pretraga") },
                        selected = false,
                        onClick = { navController.navigate("search") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
                        label = { Text("Mapa") },
                        selected = false,
                        onClick = {  navController.navigate("map")}
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "User Profile") },
                        label = { Text("Moj profil") },
                        selected = navController.currentDestination?.route == "profile",
                        onClick = { navController.navigate("profile") }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "Users") },
                        label = { Text("Korisnici") },
                        selected = navController.currentDestination?.route == "users",
                        onClick = { navController.navigate("users") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Service Control") },
                        label = { Text("Servisi") },
                        selected = navController.currentDestination?.route == "service_control",
                        onClick = { navController.navigate("service_control") }
                    )
                }
            }
        ) { innerPadding ->
            // Sadržaj ekrana
            NavHost(
                navController = navController,
                startDestination = "map",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable("map") {
                    MapScreen(navController = navController, viewModel = locationViewModel)
                }
                composable("profile") {
                    UserProfileScreen(navController=navController,onLogout=onLogout)
                }
                composable("users") {
                    UsersScreen()
                }
                composable("animals") {
                    AnimalsScreen(animalViewModel = animalViewModel, navController=navController, locationViewModel = locationViewModel)
                }
                composable("fishes") {
                    FishesScreen(fishViewModel=fishViewModel, navController=navController, locationViewModel=locationViewModel)
                }
                composable("service_control") {
                    val context = LocalContext.current
                    ServiceControlScreen(context = context)
                }
                composable("search") {
                    SearchScreen(navController=navController, locationViewModel = locationViewModel)
                }
            }
        }
    }
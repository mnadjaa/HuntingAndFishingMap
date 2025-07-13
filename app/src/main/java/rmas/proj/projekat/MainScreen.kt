package rmas.proj.projekat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Water
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import rmas.proj.projekat.data.Animal
import rmas.proj.projekat.data.AnimalViewModel
import rmas.proj.projekat.data.Fish
import rmas.proj.projekat.data.FishViewModel
import rmas.proj.projekat.data.UserViewModel
import java.util.UUID

@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf(NavigationScreen.MAP) }
    val context = LocalContext.current
    var isNavigationRailVisible by remember { mutableStateOf(false) }


    val pickImageLauncher = rememberLauncherForActivityResult(// da li ovo mogu da obrisem ❓❓❓❓
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            // Ovde možeš dodati logiku za rezultat (izbor slike)//???‼️‼️‼️‼️‼️‼️‼️‼️‼️‼️‼️‼️
        }
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // Side Navigation (Left Side)
        if (isNavigationRailVisible) {
            NavigationRail {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationRailItem(
                        icon = { Icon(Icons.Default.Pets, contentDescription = "Animals") },
                        label = { Text("Životinje") },
                        selected = currentScreen == NavigationScreen.ANIMALS,
                        onClick = { currentScreen = NavigationScreen.ANIMALS }
                    )

                    NavigationRailItem(
                        icon = { Icon(Icons.Default.Water, contentDescription = "Fishes") },
                        label = { Text("Ribe") },
                        selected = currentScreen == NavigationScreen.FISHES,
                        onClick = { currentScreen = NavigationScreen.FISHES }
                    )
                }
            }
        }

        // Main Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentScreen) {
                        NavigationScreen.MAP -> MapScreen()
                        NavigationScreen.PROFILE -> UserProfileScreen()
                        NavigationScreen.USERS -> UsersScreen()
                        NavigationScreen.SERVICE_CONTROL -> ServiceControlScreen(context)
                        NavigationScreen.ANIMALS -> AnimalsScreen()//(pickImageLauncher = pickImageLauncher)
                        NavigationScreen.FISHES -> FishesScreen()
                    }
                }

                // Bottom Navigation
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Meni") },
                        label = { Text("Meni") },
                        selected = false,
                        onClick = { isNavigationRailVisible = !isNavigationRailVisible }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                        label = {  if (!isNavigationRailVisible) Text("Mapa") },
                        selected = currentScreen == NavigationScreen.MAP,
                        onClick = { currentScreen = NavigationScreen.MAP }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "User Profile") },
                        label = { if (!isNavigationRailVisible) Text("Moj profil") },
                        selected = currentScreen == NavigationScreen.PROFILE,
                        onClick = { currentScreen = NavigationScreen.PROFILE }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "Users") },
                        label = { if (!isNavigationRailVisible) Text("Korisnici") },
                        selected = currentScreen == NavigationScreen.USERS,
                        onClick = { currentScreen = NavigationScreen.USERS }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Service Control") },
                        label = { if (!isNavigationRailVisible) Text("Servisi") },
                        selected = currentScreen == NavigationScreen.SERVICE_CONTROL,
                        onClick = { currentScreen = NavigationScreen.SERVICE_CONTROL }
                    )
                }
            }
        }
    }
}

// Enum za razlicite screenove
enum class NavigationScreen {
    MAP,
    PROFILE,
    USERS,
    ANIMALS,
    FISHES,
    SERVICE_CONTROL
}

@Composable
fun UserProfileScreen(
    userViewModel: UserViewModel = viewModel()
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val usersData by userViewModel.usersData.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val error by userViewModel.error.collectAsState()

    LaunchedEffect(currentUserId) {
        userViewModel.loadUserData(currentUserId)
    }

    val currentUser = usersData[currentUserId]

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Greška pri učitavanju profila: ${error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            currentUser != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Profile Header
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Profile Picture
                            if (currentUser.profilePicture.isNotEmpty()) {
                                AsyncImage(
                                    model = currentUser.profilePicture,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .padding(bottom = 16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Profile Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .padding(bottom = 16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // User Name
                            Text(
                                text = "${currentUser.firstName} ${currentUser.lastName}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // User Details
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Phone Number
                            Text(
                                text = "Kontakt informacije",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (currentUser.phoneNumber.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Telefon:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = currentUser.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            // User ID
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Korisnički ID:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = currentUser.id,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Korisnik nije pronađen")
                }
            }
        }
    }
}

@Composable
fun UsersScreen() {
    // Ovdje dodaj logiku za prikaz korisnika
    Text("Ekran za korisnike")
}


@Composable
fun AnimalsScreen(
    animalViewModel: AnimalViewModel = viewModel()
) {
    val selectedImageUri by animalViewModel.selectedImageUri.collectAsState()
    val animalsData by animalViewModel.animalsData.collectAsState()
    val isLoading by animalViewModel.isLoading.collectAsState()
    val error by animalViewModel.error.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()


    // Create the image picker launcher inside the composable
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { animalViewModel.updateSelectedImage(it) }
    }

    LaunchedEffect(Unit) {
        animalViewModel.loadAllAnimals()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Greška pri učitavanju: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Izbor slike
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { pickImageLauncher.launch("image/*") }
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Slika životinje",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Dodaj sliku",
                                modifier = Modifier.size(40.dp).align(Alignment.Center) // Centriranje ikone unutar Box-a

                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    var name by remember { mutableStateOf("") }
                    var description by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Naziv životinje") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Opis životinje") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (name.isEmpty() || description.isEmpty() || selectedImageUri == null) {
                                Toast.makeText(
                                    context,
                                    "Popunite sva polja i izaberite sliku!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val newAnimal = Animal(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                photo = selectedImageUri.toString(),
                                authorId = "test_author"
                            )

                            coroutineScope.launch {
                                animalViewModel.createAnimal(newAnimal,context)
                                Toast.makeText(context, "Životinja dodata!", Toast.LENGTH_SHORT).show()
                                // Resetovanje slike nakon dodavanja životinje
                                animalViewModel.updateSelectedImage(null)


                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dodaj životinju")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(animalsData.values.toList()) { animal ->
                            AnimalItem(animal)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AnimalItem(animal: Animal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (animal.photo.isNotEmpty()) {
                AsyncImage(
                    model = animal.photo,
                    contentDescription = "Slika životinje",
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Text(text = animal.name, style = MaterialTheme.typography.titleLarge)
            Text(text = animal.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


@Composable
fun FishesScreen(
    fishViewModel: FishViewModel = viewModel()
) {
    val selectedImageUri by fishViewModel.selectedImageUri.collectAsState()
    val fishesData by fishViewModel.fishData.collectAsState()
    val isLoading by fishViewModel.isLoading.collectAsState()
    val error by fishViewModel.error.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Create the image picker launcher inside the composable
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fishViewModel.updateSelectedImage(it) }
    }

    LaunchedEffect(Unit) {
        fishViewModel.loadAllFishes()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Greška pri učitavanju: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image Selection
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { pickImageLauncher.launch("image/*") }
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Slika ribe",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Dodaj sliku",
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var name by remember { mutableStateOf("") }
                    var description by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Naziv ribe") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Opis ribe") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (name.isEmpty() || description.isEmpty() || selectedImageUri == null) {
                                Toast.makeText(
                                    context,
                                    "Popunite sva polja i izaberite sliku!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val newFish = Fish(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                photo = selectedImageUri.toString(),
                                authorId = "test_author"
                            )

                            coroutineScope.launch {
                                fishViewModel.createFish(newFish, context)
                                Toast.makeText(context, "Riba dodata!", Toast.LENGTH_SHORT).show()
                                // Reset image after adding fish
                                fishViewModel.updateSelectedImage(null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dodaj ribu")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(fishesData.values.toList()) { fish ->
                            FishItem(fish)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FishItem(fish: Fish) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fish.photo.isNotEmpty()) {
                AsyncImage(
                    model = fish.photo,
                    contentDescription = "Slika ribe",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Text(text = fish.name, style = MaterialTheme.typography.titleLarge)
            Text(text = fish.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}



@Composable
fun ServiceControlScreen(context: Context) {//🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                val intent = Intent(context, TrackingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent) // API 26+
                } else {
                    context.startService(intent) // API 24-25
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Pokreni Servis")
        }

        Button(
            onClick = {
                val intent = Intent(context, TrackingService::class.java)
                context.stopService(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zaustavi Servis")
        }
    }
}
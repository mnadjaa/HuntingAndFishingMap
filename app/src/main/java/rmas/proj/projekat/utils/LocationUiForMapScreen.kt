package rmas.proj.projekat.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import rmas.proj.projekat.data.Location
import rmas.proj.projekat.data.LocationType
import rmas.proj.projekat.data.Rating
import rmas.proj.projekat.data.SpeciesType
import rmas.proj.projekat.viewmodel.LocationViewModel
import rmas.proj.projekat.viewmodel.UserViewModel
import rmas.proj.projekat.viewmodel.SpeciesViewModel

@Composable
fun LocationDetailsDialog( //prikazuje sve informacije o selektovanoj lokaciji
    location: Location,
    currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "",
    context: Context,
    onDismiss: () -> Unit,
    onRatingAdded: (Rating) -> Unit,
    onRatingUpdated: (String, Rating) -> Unit,
    onLocationUpdated: (Location) -> Unit,
    viewModel: LocationViewModel = viewModel()
) {
    var rating by remember { mutableStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var showAddRatingDialog by remember { mutableStateOf(false) }
    val userViewModel: UserViewModel = viewModel()
    val speciesViewModel: SpeciesViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val animalsAtLocation by viewModel.animalsAtLocation.collectAsState()
    val fishAtLocation by viewModel.fishAtLocation.collectAsState()
    val availableAnimals by viewModel.availableAnimals.collectAsState()
    val availableFish by viewModel.availableFish.collectAsState()
    var showAddAnimalDialog by remember { mutableStateOf(false) }
    var showAddFishDialog by remember { mutableStateOf(false) }
    var showRatings by remember { mutableStateOf(false) }
    val existingRating = location.ratings.find { it.userId == currentUserId }    //za ulogovanog korisnika
    val sightings by speciesViewModel.locationSightings.collectAsState()

    LaunchedEffect(Unit) {
        // Clear any previously selected image when opening the dialog
        viewModel.updateSelectedImage(null)
        // Ucitaj detalje lokacije sa zivotinjama/ribama
        viewModel.loadLocationDetails(location.id)
    }
    LaunchedEffect(location.id) {
        speciesViewModel.loadSightingsForLocation(location.id)
    }

    Log.d("DEBUG", "Lokacija otvorena u dialogu: ${location.photos}")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateSelectedImage(it)
        }
    }
    // ucitavaju se podaci o korisnicima
    LaunchedEffect(location.ratings) {
        location.ratings.forEach { existingRating ->
            if (!userViewModel.usersData.value.containsKey(existingRating.userId)) {
                userViewModel.loadUserData(existingRating.userId)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Prikaz tipa lokacije i x dugme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(
                        imageVector = when(location.type) {
                            LocationType.HUNTING -> Icons.Default.Pets
                            LocationType.FISHING -> Icons.Default.Water
                        },
                        contentDescription = location.type.label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zatvori"
                        )
                    }
                }
                //ime i opis
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = location.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                //dodavanje zivotinje i lista zivotinja na lokaciji
                if (location.type == LocationType.HUNTING) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dodaj životinju (${animalsAtLocation.size})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { showAddAnimalDialog = true }) {
                            Icon(Icons.Default.Add, "Dodaj životinju")
                        }
                    }

                    if (animalsAtLocation.isEmpty()) {
                        Text(
                            text = "Nema dodatih životinja",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        SpeciesSection(
                            speciesList = animalsAtLocation,
                            sightings = sightings,
                            title = "Životinje na lokaciji",
                            onSeenClick = { speciesId ->
                                speciesViewModel.updateLastSeen(speciesId, location.id)
                            }
                        )
                    }
                }
                //dodavanje ribe i lista riba na lokaciji
                if (location.type == LocationType.FISHING) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dodaj ribu (${fishAtLocation.size})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { showAddFishDialog = true }) {
                            Icon(Icons.Default.Add, "Dodaj ribu")
                        }
                    }

                    if (fishAtLocation.isEmpty()) {
                        Text(
                            text = "Nema dodatih riba",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        SpeciesSection(
                            speciesList = fishAtLocation,
                            sightings = sightings,
                            title = "Ribe na lokaciji",
                            onSeenClick = { speciesId ->
                                speciesViewModel.updateLastSeen(speciesId, location.id)
                            }
                        )
                    }
                }
                // slike lokacije, mogucnost dodavanja slike
                if (location.photos.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dodaj sliku",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 0.dp) // razmak izmedju teksta i ikone 8
                        )
                        IconButton(
                            onClick = { launcher.launch("image/*") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Dodaj sliku"
                            )
                        }
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    )
                    {
                        // Show selected image if exists
                        if (selectedImageUri != null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)  // Ograničavamo veličinu slike
                                        .padding(end = 8.dp),
                                    contentAlignment = Alignment.BottomCenter // Dugme ide na dno
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Nova slika",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Dugme za potvrdu dodavanja slike
                                    Button(
                                        onClick = {
                                            val currentUri = selectedImageUri
                                            if (currentUri != null) {
                                                scope.launch {
                                                    try {
                                                        val imageUrl = uploadLocationImage(context, currentUri)
                                                        val updatedPhotos = location.photos + imageUrl
                                                        val updatedLocation = location.copy(photos = updatedPhotos)
                                                        onLocationUpdated(updatedLocation)
                                                        viewModel.updateSelectedImage(null) // Obriši selektovanu sliku
                                                        Toast.makeText(context, "Slika uspešno dodata", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Greška pri dodavanju slike: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp) // Razmak između dugmeta i slike
                                    ) {
                                        Text(
                                            text = "Dodaj sliku",
                                            textAlign = TextAlign.Center, // Centriranje teksta unutar dugmeta
                                            modifier = Modifier.fillMaxWidth() // Osigurava da tekst koristi punu širinu dugmeta
                                        )                                    }
                                }
                            }
                        }
                        // Show existing photos
                        items(location.photos) { photoUrl ->
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .padding(end = 8.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                //prosecna ocena
                val averageRating = if (location.ratings.isNotEmpty()) {
                    location.ratings.map { it.score }.average().toFloat()
                } else {
                    0f
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prosečna ocena: ",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format("%.1f", averageRating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/5",
                        style = MaterialTheme.typography.titleMedium
                    )

                }
                // Deo za dodavanje ocene i prikaz ocena
                Text(
                    text = "Ocene (${location.ratings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Button(
                    onClick = { showRatings = !showRatings },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showRatings) "Sakrij ocene" else "Prikaži ocene")
                }
                if(showRatings){//prikazuju se sve ocene
                    location.ratings.forEach { existingRating ->
                        RatingItem(
                            rating = existingRating,
                            isCurrentUser = existingRating.userId == currentUserId
                        )
                    }
                }
                // --------- AUTO POPUNJAVANJE - ako je korisnik vec ocenio tu lokaciju tu stoji njegova stara ocena---------
                LaunchedEffect(showAddRatingDialog, existingRating) {
                    if (showAddRatingDialog && existingRating != null) {
                        rating = existingRating.score
                        comment = existingRating.comment
                    }
                    if (showAddRatingDialog && existingRating == null) {
                        rating = 0f
                        comment = ""
                    }
                }
                // dodavanje/izmena ocene
                Button(
                    onClick={showAddRatingDialog=!showAddRatingDialog},
                    modifier = Modifier.fillMaxWidth()
                ){
                    Text(
                        text = when {
                            showAddRatingDialog -> "Odustani"
                            existingRating != null -> "Promeni ocenu"
                            else -> "Dodaj ocenu"
                        }
                    )
                }
                if(showAddRatingDialog){
                    Text(
                        text = if (existingRating != null) "Promeni ocenu" else "Dodaj ocenu",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    Slider(
                        value = rating,
                        onValueChange = { rating = it },
                        valueRange = 0f..5f,
                        steps = 4
                    )

                    Text(
                        text = "Ocena: ${rating.toInt()}/5",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(if (existingRating != null) "Promeni komentar" else "Dodaj komentar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val newRating = Rating(
                                userId = currentUserId,
                                score = rating,
                                comment = comment
                            )
                            if (existingRating != null) { //ne dodaju se poeni ako se samo menja ocena
                                onRatingUpdated(location.id, newRating)
                                onDismiss()
                            } else { //dodaju se poeni za novu ocenu
                                onRatingAdded(newRating)
                                scope.launch {
                                    try {
                                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                                        if (userId == null) {
                                            Log.e("POINTS_DEBUG", "Korisnik nije ulogovan")
                                            return@launch
                                        }
                                        Log.d("POINTS_DEBUG", "Stvarni userId: $userId")
                                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        db.collection("users")
                                            .document(userId)
                                            .update("points", com.google.firebase.firestore.FieldValue.increment(3))
                                            .await()
                                        Log.d("POINTS_DEBUG", "Poeni uspesno dodati")
                                    } catch (e: Exception) {
                                        Log.e("POINTS_DEBUG", "Greska: ${e.message}")
                                    }finally {
                                        onDismiss() // zatvaramo dijalog tek kad je Firestore završio
                                    }
                                }
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        enabled = rating > 0f
                    ) {
                        Text(if (existingRating != null) "Promeni ocenu" else "Oceni")
                    }
                }
            }
        }
    }

    if (showAddAnimalDialog) {
        AddSpeciesToLocationDialog(
            availableSpecies = availableAnimals,
            title = "Izaberi životinju",
            onDismiss = { showAddAnimalDialog = false },
            onSpeciesSelected = { speciesId ->
                viewModel.addSpeciesToLocation(location.id, speciesId, SpeciesType.ANIMAL)
                showAddAnimalDialog = false
            }
        )
    }

    if (showAddFishDialog) {
        AddSpeciesToLocationDialog(
            availableSpecies = availableFish,
            title = "Izaberi ribu",
            onDismiss = { showAddFishDialog = false },
            onSpeciesSelected = { speciesId ->
                viewModel.addSpeciesToLocation(location.id, speciesId, SpeciesType.FISH)
                showAddFishDialog = false
            }
        )
    }
}

@Composable
fun RatingItem(
    rating: Rating,
    isCurrentUser: Boolean,
    userViewModel: UserViewModel = viewModel()
) {
    val usersData by userViewModel.usersData.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val error by userViewModel.error.collectAsState()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    LaunchedEffect(currentUserId) {
        userViewModel.loadUserData(currentUserId)
    }
    val currentUser = usersData[currentUserId]

    val ratingUserId = rating.userId//onaj koji je ostavio ocenu
    LaunchedEffect(ratingUserId) {// Load user data for the user who left the rating
        userViewModel.loadUserData(ratingUserId)
    }
    val ratingUser = usersData[ratingUserId]


    val userName = when {
        isCurrentUser -> {
            // za tren korisnika se prikazuje Vi (ime+prezime)
            currentUser?.let { "Vi (${it.firstName} ${it.lastName})" } ?: "Vi"
        }
        isLoading -> "Učitavanje..."
        error != null -> "Greška pri učitavanju"
        ratingUser != null -> "${ratingUser.firstName} ${ratingUser.lastName}"
        else -> "Nepoznat korisnik"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Korisnik: $userName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${rating.score.toInt()}/5",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = rating.comment,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = formatTimestamp(rating.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
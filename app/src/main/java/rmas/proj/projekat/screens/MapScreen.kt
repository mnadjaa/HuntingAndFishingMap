package rmas.proj.projekat.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import kotlinx.coroutines.launch
import rmas.proj.projekat.data.Location
import rmas.proj.projekat.data.LocationType
import rmas.proj.projekat.data.Rating
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.Icon
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.ui.layout.ContentScale
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import androidx.compose.foundation.layout.Row
import androidx.navigation.NavController
import rmas.proj.projekat.utils.LocationDetailsDialog
import rmas.proj.projekat.viewmodel.LocationViewModel
import rmas.proj.projekat.utils.getCurrentLocation
import rmas.proj.projekat.utils.uploadLocationImage
//import rmas.proj.projekat.utils.setupLocationUpdates
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    navController: NavController,//obrisi
    viewModel: LocationViewModel
) {
    val context = LocalContext.current
    val fusedLocationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showLocationsTable by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    val filteredLocations by viewModel.filteredLocations.collectAsState()
    val isFilteringActive by viewModel.isFilteringActive.collectAsState()
    val allLocations by viewModel.locations.collectAsState()
    val selectedLocationType by viewModel.selectedLocationType.collectAsState()

    val locationsToShow = remember(allLocations, filteredLocations, isFilteringActive, selectedLocationType) {
        if (isFilteringActive) {
            filteredLocations
        } else {
            allLocations.values.toList()
        }
    }

    //------------------------------------------------------------------------
    LaunchedEffect(isFilteringActive, locationsToShow.size) {
        if (isFilteringActive) {
            Log.d("LocationViewModel", "Filtering active - showing ${locationsToShow.size} filtered locations")
        } else {
            Log.d("LocationViewModel", "Loaded - showing all ${locationsToShow.size}")
        }
    }
    LaunchedEffect(locationsToShow, isFilteringActive) {
        Log.d(
            "DEBUG_MAP",
            "isFilteringActive=$isFilteringActive, locationsToShow=${locationsToShow.size}, filteredLocations=${filteredLocations.size}, allLocations=${allLocations.size}"
        )
    }
    //-----------------------------------------------------------------------------
    var characterPosition = viewModel.characterPosition
    val characterMarkerState = remember { mutableStateOf(MarkerState(position = userLocation ?: LatLng(0.0, 0.0))) }
    //var showMain by remember { mutableStateOf(false) }
    //var isNavigationRailVisible by remember { mutableStateOf(false) }

    // Remember location callback
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng
                    viewModel.setUserLocation(latLng)
                }
            }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            permissionGranted = granted
            if (granted) {
                getCurrentLocation(fusedLocationProvider) { location ->
                    userLocation = location
                    viewModel.setUserLocation(location)
                    viewModel.initializeCharacterPosition(location)
                    characterMarkerState.value = MarkerState(position = location)
                    //setupLocationUpdates(fusedLocationProvider, locationCallback)
                }
            }
        }
    )

    fusedLocationProvider.requestLocationUpdates(
        com.google.android.gms.location.LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        },
        locationCallback,
        android.os.Looper.getMainLooper()
    )

    // Cleanup kad se izlazi iz ekrana
    DisposableEffect(Unit) {
        onDispose {
            fusedLocationProvider.removeLocationUpdates(locationCallback)
        }
    }

    // Provera da li je dozvola odobrena
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            permissionGranted = true
            getCurrentLocation(fusedLocationProvider) { location ->
                userLocation = location
                viewModel.initializeCharacterPosition(location)
                characterMarkerState.value = MarkerState(position = location)
                //setupLocationUpdates(fusedLocationProvider, locationCallback)
            }
        } else {
            locationPermissionLauncher.launch(permission)
        }
    }

    val cameraPositionState = rememberCameraPositionState()    // azuriranje pozicije kamere
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(it, 15f),
                durationMs = 1000
            )
        }
    }

    Box(modifier = Modifier.fillMaxHeight()) {
                if (!permissionGranted) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Dozvola za lokaciju je potrebna da bi mapa mogla da se prikaže.")//Location permission is required to show the map
                    }
                } else if (userLocation == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Preuzimanje Vaše lokacije...")//Fetching your location...
                    }
                } else {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true
                        )
                    ) {
                        locationsToShow.forEach { location ->
                            key(location.id) {
                                Marker(
                                    state = MarkerState(position = location.position),
                                    title = location.name,
                                    snippet = location.description,
                                    onClick = {
                                        selectedLocation = location
                                        true
                                    }
                                )
                            }
                        }
                        characterPosition?.let { position ->
                            Marker(
                                state = characterMarkerState.value,
                                title = "Moj karakter",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // btn za otvaranje tabele lokacija
                        FloatingActionButton(
                            onClick = { showLocationsTable = true },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Prikaži tabelu lokacija"
                            )
                        }
                        // btn za dodavanje nove lokacije
                        FloatingActionButton(
                            onClick = { showAddLocationDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Dodaj lokaciju")
                        }
                    }

                }
            }

    if (showLocationsTable) {
        Dialog(onDismissRequest = { showLocationsTable = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                shape = RoundedCornerShape(16.dp)
            ) {
                LocationsTableSheet(
                    locations = locationsToShow,
                    userLocation = userLocation
                )
            }
        }
    }
    // Dijalozi
    if (showAddLocationDialog) {
        AddLocationDialog(
            //currentLocation = userLocation ?: LatLng(0.0, 0.0),//ovo dodaje lokaciju sa tren lokacije a ovo ispod sa lokac pokretnog markera🐛DA LI SE DOBRO DODAJE U OBA SLUCAJA
            currentLocation = characterPosition ?: userLocation ?: LatLng(0.0, 0.0),
            context = context,
            onDismiss = { showAddLocationDialog = false },
            onLocationAdded = { location : Location->
                viewModel.addLocation(location,context)
                showAddLocationDialog = false
            }
        )
    }
    selectedLocation?.let { location ->
        LocationDetailsDialog(
            location = location,
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            context = context,
            onDismiss = { selectedLocation = null },
            onRatingAdded = { rating : Rating->
                viewModel.addRating(location.id, rating,context)
            },
            onRatingUpdated = { locationId :String, rating : Rating->
                viewModel.updateRating(locationId, rating,context)
            },
            onLocationUpdated = { updatedLocation ->
                viewModel.updateLocation(updatedLocation)
            }
        )
    }
}
@Composable
fun AddLocationDialog(
    currentLocation: LatLng,
    context: Context,
    onDismiss: () -> Unit,
    onLocationAdded: (Location) -> Unit,
    viewModel: LocationViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LocationType.HUNTING) }
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateSelectedImage(it)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Location Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Location Image",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naziv lokacije") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                LocationTypeSelector(
                    selectedType = type,
                    onTypeSelected = { type = it }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Otkaži")
                    }
                    Button(
                        onClick = {
                            val currentUri = selectedImageUri
                            if (currentUri != null) {
                                scope.launch {
                                    try {
                                        val imageUrl = uploadLocationImage(context, currentUri)
                                        Log.d("DEBUG", "Uploadovan URL slike: $imageUrl")
                                        val location = Location(
                                            name = name,
                                            description = description,
                                            type = type,
                                            position = currentLocation,
                                            photos = listOf(imageUrl),
                                            authorId = currentUserId
                                        )
                                        Log.d("DEBUG", "Nova lokacija: $location")
                                        onLocationAdded(location)
                                        viewModel.updateSelectedImage(null) // Clear the selected image jer mi se posle pokazivala slika koju sam dodala kao da je selektovana i nudio mi je da je dodam u locationdetailsdialogu
                                        onDismiss()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Error uploading image: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Odaberite sliku",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                                  },
                                enabled = name.isNotBlank() && description.isNotBlank()
                            ) {
                                Text("Dodaj lokaciju")
                              }
                }
            }
        }
    }
}

@Composable
fun LocationTypeSelector(
    selectedType: LocationType,
    onTypeSelected: (LocationType) -> Unit
) {
    Column {
        Text(
            text = "Location Type",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LocationType.values().forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = type == selectedType,
                        onClick = { onTypeSelected(type) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = type == selectedType,
                    onClick = { onTypeSelected(type) }
                )
                Text(
                    text = type.label,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

//prikaz lokacija u tabeli:
@Composable
fun LocationsTableSheet(
    locations: List<Location>,
    userLocation: LatLng?
) {
    fun distanceKm(loc: Location): Double? {
        userLocation ?: return null
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            userLocation.latitude, userLocation.longitude,
            loc.position.latitude, loc.position.longitude,
            results
        )
        return results[0] / 1000.0
    }

    val sorted = remember(locations, userLocation) {
        locations.sortedBy { distanceKm(it) ?: Double.MAX_VALUE }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Lokacije u okolini",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // zaglavlje tabele
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Naziv",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(2f)
            )
            Text(
                text = "Tip",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Udaljenost",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }

        HorizontalDivider()

        // redovi tabele
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            itemsIndexed(sorted) { index, location ->
                val distance = distanceKm(location)
                val distanceText = when {
                    distance == null -> "—"
                    distance < 1.0 -> "${(distance * 1000).toInt()} m"
                    else -> "${"%.1f".format(distance)} km"
                }
                val typeLabel = when (location.type) {
                    LocationType.HUNTING -> "Lov"
                    LocationType.FISHING -> "Ribolov"
                }
                val typeColor = when (location.type) {
                    LocationType.HUNTING -> MaterialTheme.colorScheme.tertiary
                    LocationType.FISHING -> MaterialTheme.colorScheme.primary
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (index % 2 == 0) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = typeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = typeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}
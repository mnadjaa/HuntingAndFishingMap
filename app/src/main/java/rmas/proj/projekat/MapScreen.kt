package rmas.proj.projekat

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.StateFlow
import rmas.proj.projekat.data.LocationRepository
import rmas.proj.projekat.data.UserViewModel
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Menu
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Close

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    navController: NavController,//valjda mi ne treba
    viewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val fusedLocationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) } // State za dijaloge
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    val locations by viewModel.filteredLocations.collectAsState()
    var characterPosition = viewModel.characterPosition
    val characterMarkerState = remember { mutableStateOf(MarkerState(position = userLocation ?: LatLng(0.0, 0.0))) }

    var showMain by remember { mutableStateOf(false) }

    //📌
    val isManuallyControlling by viewModel.isManuallyControlling.collectAsState()

    // Remember location callback
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng

                    // updateuje se pozicija ako nije ukljuceno manuallycontrolling
                    if (!isManuallyControlling) {
                        viewModel.updatePosition(latLng)
                        characterMarkerState.value = MarkerState(position = latLng)
                    }
                }
            }
        }
    }//kraj📌

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            permissionGranted = granted
            if (granted) {
                getCurrentLocation(fusedLocationProvider) { location ->
                    userLocation = location
                    //pokretni marker init:
                    viewModel.initializeCharacterPosition(location)
                    characterMarkerState.value = MarkerState(position = location)
                    setupLocationUpdates(fusedLocationProvider, locationCallback)// Start location updates📌
                }
            }
        }
    )

    //📌
    // Cleanup location updates when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            fusedLocationProvider.removeLocationUpdates(locationCallback)
        }
    }//📌

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
                // init pokretni marker
                viewModel.initializeCharacterPosition(location)
                characterMarkerState.value = MarkerState(position = location)
                setupLocationUpdates(fusedLocationProvider, locationCallback) // Start location updates📌
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

    // Glavni UI
    Box(modifier = Modifier.fillMaxSize()) {
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
                // Marker za trenutnu lokaciju//ovo mi vise ne treba
                /*Marker(
                    state = rememberMarkerState(position = userLocation!!),
                    title = "Trenutna lokacija"
                )*/

                // Markeri za sve lokacije
                locations.forEach { location ->
                    Marker(
                        state = rememberMarkerState(position = location.position),
                        title = location.name,
                        snippet = location.description,
                        onClick = {
                            selectedLocation = location
                            true
                        }
                    )
                }

                //pokretni marker
                characterPosition?.let { position ->
                    Marker(
                        state = characterMarkerState.value,
                        title = "Moj karakter",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }

            FloatingActionButton(
                onClick = { showMain = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Otvori meni")
            }

            // btn za dodavanje nove lokacije
            FloatingActionButton(
                onClick = { showAddLocationDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj lokaciju")
            }

            //pokretni marker strelice za pomeranje
            characterPosition?.let { position ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 0.dp),//80
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        )
                        {
                            // Toggle button for manual control//📌
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text("Ručno upravljanje: ")
                                Switch(
                                    checked = isManuallyControlling,
                                    onCheckedChange = { viewModel.setManualControl(it) }
                                )
                            }
                            // Only show arrow controls when manual control is enabled
                            if (isManuallyControlling) {
                                IconButton(
                                    onClick = {
                                        val newPosition = LatLng(position.latitude + 0.0001, position.longitude)//mogu da povecam da se vise pomera 🐛
                                        viewModel.updatePosition(newPosition)
                                        characterMarkerState.value = MarkerState(position = newPosition)
                                        // Mock the user location
                                        mockUserLocation(context, newPosition)
                                    }
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, "Gore")
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            val newPosition = LatLng(position.latitude, position.longitude - 0.0001)
                                            viewModel.updatePosition(newPosition)
                                            characterMarkerState.value = MarkerState(position = newPosition)
                                            // Mock the user location
                                            mockUserLocation(context, newPosition)
                                        }
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowLeft, "Levo")
                                    }

                                    Spacer(modifier = Modifier.width(24.dp))

                                    IconButton(
                                        onClick = {
                                            val newPosition = LatLng(position.latitude, position.longitude + 0.0001)
                                            viewModel.updatePosition(newPosition)
                                            characterMarkerState.value = MarkerState(position = newPosition)
                                            // Mock the user location
                                            mockUserLocation(context, newPosition)
                                        }
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowRight, "Desno")
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val newPosition = LatLng(position.latitude - 0.0001, position.longitude)
                                        viewModel.updatePosition(newPosition)
                                        characterMarkerState.value = MarkerState(position = newPosition)
                                        // Mock the user location
                                        mockUserLocation(context, newPosition)
                                    }
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, "Dole")
                                }
                            }//kraj📌
                        }
                    }
                }
            }
        }
    }

    if (showMain) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {

            MainScreen(navController = navController)

            IconButton(
                onClick = { showMain = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Zatvori")
            }
        }
    }

    // Dijalozi
    if (showAddLocationDialog) {
        AddLocationDialog(
            //currentLocation = userLocation ?: LatLng(0.0, 0.0),//ovo dodaje lokaciju sa tren lokacije a ovo ispod sa lokac pokretnog markera
            currentLocation = characterPosition ?: userLocation ?: LatLng(0.0, 0.0),//🦋
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
            currentUserId = "current_user_id",//🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛🐛ne treba ovako?ako ne radi ovako probaj da iznad locationsdialog dodas ovo val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest" i onda umesto ove linije ovo:  currentUserId = currentUserId,
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


// Funkcija za dobijanje trenutne lokacije
@SuppressLint("MissingPermission")
fun getCurrentLocation(
    fusedLocationProvider: FusedLocationProviderClient,
    onLocationReceived: (LatLng) -> Unit
) {
    fusedLocationProvider.lastLocation
        .addOnSuccessListener { location ->
            location?.let {
                onLocationReceived(LatLng(it.latitude, it.longitude))
            }
        }
        .addOnFailureListener {
            println("Nije uspelo pribavljanje lokacije")//Nije moguće dobiti lokaciju
        }
}



// ViewModel za dodavanje objekata, ocena i filtriranje obj
class LocationViewModel(
    private val locationRepository: LocationRepository = LocationRepository()
) : ViewModel() {

    private val _locations = MutableStateFlow<Map<String, Location>>(emptyMap())
    val locations = _locations.asStateFlow()

    private val _filteredLocations = MutableStateFlow<List<Location>>(emptyList())
    val filteredLocations = _filteredLocations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()


    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    //pokretni marker
    private val _characterPosition = MutableStateFlow<LatLng?>(null)
    var characterPosition by mutableStateOf<LatLng?>(null)
        private set

    fun initializeCharacterPosition(position: LatLng) {//pokretni marker
        characterPosition = position
        _characterPosition.value = position
    }

    fun updatePosition(newPosition: LatLng) {//pokretni marker
        characterPosition = newPosition
        _characterPosition.value = newPosition
    }


    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri = _selectedImageUri.asStateFlow()

    fun updateSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    init {
        loadAllLocations()
    }

    private fun loadAllLocations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            locationRepository.getAllLocations()
                .onSuccess { locationsList ->
                    _locations.value = locationsList.associateBy { it.id }
                    applyFilters()
                    Log.d("LocationViewModel", "Successfully loaded ${locationsList.size} locations")
                }
                .onFailure { exception ->
                    _error.value = "Error loading locations: ${exception.message}"
                    Log.e("LocationViewModel", "Error loading locations", exception)
                }

            _isLoading.value = false
        }
    }

    fun addLocation(location: Location, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            locationRepository.createLocation(location)
                .onSuccess { createdLocation ->
                    _locations.value = _locations.value + (createdLocation.id to createdLocation)
                    applyFilters()
                    Toast.makeText(context, "Location successfully added", Toast.LENGTH_SHORT).show()
                    Log.d("LocationViewModel", "Successfully created location: ${createdLocation.id}")
                }
                .onFailure { exception ->
                    _error.value = "Error creating location: ${exception.message}"
                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LocationViewModel", "Error creating location", exception)
                }

            _isLoading.value = false
        }
    }



    fun updateLocation(location: Location) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            locationRepository.updateLocation(location)
                .onSuccess { updatedLocation ->
                    _locations.value = _locations.value + (updatedLocation.id to updatedLocation)
                    applyFilters()
                    Log.d("LocationViewModel", "Successfully updated location: ${updatedLocation.id}")
                }
                .onFailure { exception ->
                    _error.value = "Error updating location: ${exception.message}"
                    Log.e("LocationViewModel", "Error updating location", exception)
                }

            _isLoading.value = false
        }
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            locationRepository.deleteLocation(locationId)
                .onSuccess {
                    _locations.value = _locations.value - locationId
                    applyFilters()
                    Log.d("LocationViewModel", "Successfully deleted location: $locationId")
                }
                .onFailure { exception ->
                    _error.value = "Error deleting location: ${exception.message}"
                    Log.e("LocationViewModel", "Error deleting location", exception)
                }

            _isLoading.value = false
        }
    }

    fun addRating(locationId: String, rating: Rating, context: Context) {
            viewModelScope.launch {
                try {
                    val locationToUpdate = _locations.value[locationId]
                    if (locationToUpdate?.ratings?.any { it.userId == rating.userId } == true) {
                        Toast.makeText(
                            context,
                            "Već ste ocenili ovu lokaciju",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }


                    val updatedLocations = _locations.value.mapValues { (id, location) ->
                        if (id == locationId) {
                            location.copy(
                                ratings = location.ratings + rating,
                                lastInteraction = System.currentTimeMillis()
                            )
                        } else location
                    }
                    _locations.value = updatedLocations
                    applyFilters()
                    Toast.makeText(context, "Ocena uspešno dodata", Toast.LENGTH_SHORT).show()

                    locationRepository.updateLocation(updatedLocations[locationId]!!)// Save the updated location to Firestore

                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun updateRating(
            locationId: String,
            updatedRating: Rating,
            context: Context
        ) {
            viewModelScope.launch {
                try {
                    val updatedLocations = _locations.value.mapValues { (id, location) ->
                        if (id == locationId) {
                            location.copy(
                                ratings = location.ratings.map { rating ->
                                    if (rating.userId == updatedRating.userId) updatedRating else rating
                                },
                                lastInteraction = System.currentTimeMillis()
                            )
                        } else location
                    }
                    _locations.value = updatedLocations
                    applyFilters()

                    Toast.makeText(context, "Ocena uspešno promenjena", Toast.LENGTH_SHORT)
                        .show()

                    locationRepository.updateLocation(updatedLocations[locationId]!!)//Save the updated location to Firestore

                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    fun applyFilters(
        authorId: String? = null,
        type: LocationType? = null,
        dateRange: ClosedRange<Long>? = null,
        searchQuery: String = ""
    ) {
        val filtered = _locations.value.values.filter { location ->
            val matchesAuthor = authorId == null || location.authorId == authorId
            val matchesType = type == null || location.type == type
            val matchesDate = dateRange == null || location.createdAt in dateRange
            val matchesSearch = location.name.contains(searchQuery, ignoreCase = true) ||
                    location.description.contains(searchQuery, ignoreCase = true)

            matchesAuthor && matchesType && matchesDate && matchesSearch
        }
        _filteredLocations.value = filtered
    }
    fun clearError() {
        _error.value = null
    }


    // 📌
    private var locationUpdateActive = false
    lateinit var locationCallback: LocationCallback
    private val _isManuallyControlling = MutableStateFlow(false)
    val isManuallyControlling: StateFlow<Boolean> = _isManuallyControlling.asStateFlow()

    fun setManualControl(isManual: Boolean) {
        _isManuallyControlling.value = isManual
    }

    fun setupLocationUpdates(fusedLocationProvider: FusedLocationProviderClient) {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000  // Update every 5 seconds
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    characterPosition = latLng
                    _characterPosition.value = latLng
                }
            }
        }

        fusedLocationProvider.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        locationUpdateActive = true
    }

    fun removeLocationUpdates(fusedLocationProvider: FusedLocationProviderClient) {
        if (locationUpdateActive) {
            fusedLocationProvider.removeLocationUpdates(locationCallback)
            locationUpdateActive = false
        }
    }//📌

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
    var type by remember { mutableStateOf(LocationType.HUNTING) }//da li ovo treba da promenim❓❓❓❓❓❓❓❓❓❓❓❓❓❓
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

                    contentAlignment = Alignment.Center  // Ovo je ispravna opcija za centriranje
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

suspend fun uploadLocationImage(context: Context, imageUri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "temp_location_image.jpg")
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
            result["secure_url"] as String
        } catch (e: Exception) {
            throw e
        }
    }
}


@Composable
fun LocationDetailsDialog(
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

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val scope = rememberCoroutineScope()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()


    // Add this LaunchedEffect to clear the selected image when the dialog opens
    LaunchedEffect(Unit) {
        // Clear any previously selected image when opening the dialog
        viewModel.updateSelectedImage(null)
    }


    Log.d("DEBUG", "Lokacija otvorena u dialogu: ${location.photos}")


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateSelectedImage(it)
        }
    }

    // Preload user data for ratings
    LaunchedEffect(location.ratings) {
        location.ratings.forEach { existingRating ->
            if (!userViewModel.usersData.value.containsKey(existingRating.userId)) {
                userViewModel.loadUserData(existingRating.userId)
            }
        }
    }

    val existingRating = location.ratings.find { it.userId == currentUserId }    //Find existing rating for current user

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Prikaz tipa lokacije
                Text(
                    text = location.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = location.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                // Photos section
                if (location.photos.isNotEmpty()) {
                    Text(
                        text = "Slike",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

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

                // Ratings section
                Text(
                    text = "Ocene (${location.ratings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                // Display existing ratings
                location.ratings.forEach { existingRating ->
                    RatingItem(
                        rating = existingRating,
                        isCurrentUser = existingRating.userId == currentUserId
                    )
                }

                // Add/Update rating section
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
                        if (existingRating != null) {
                            onRatingUpdated(location.id, newRating)
                        } else {
                            onRatingAdded(newRating)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    enabled = rating > 0f && comment.isNotBlank()
                ) {
                    Text(if (existingRating != null) "Promeni ocenu" else "Oceni")
                }
            }
        }
    }
}

@Composable
private fun RatingItem(
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

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
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



//📌:

// 2. Add this function to setup location updates
@SuppressLint("MissingPermission")
private fun setupLocationUpdates(
    fusedLocationProvider: FusedLocationProviderClient,
    locationCallback: LocationCallback
) {
    val locationRequest = LocationRequest.create().apply {
        interval = 5000  // Update every 5 seconds
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    fusedLocationProvider.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
    )
}


// Function to mock user location (requires Location Edit app or another mock location app)
// Function to mock user location using Loca Edit app
fun mockUserLocation(context: Context, position: LatLng) {
    try {
        // Create intent to communicate with Loca Edit
        val intent = Intent("com.miramire.locaedit.SET_LOCATION")
        intent.putExtra("latitude", position.latitude)
        intent.putExtra("longitude", position.longitude)
        intent.putExtra("accuracy", 3.0f) // Accuracy in meters

        // Try to send the intent
        context.sendBroadcast(intent)

        /*Toast.makeText(
            context,
            "Lokacija postavljena na: ${position.latitude}, ${position.longitude}",
            Toast.LENGTH_SHORT
        ).show()*/
    } catch (e: Exception) {
        Log.e("MockLocation", "Error sending location to Loca Edit: ${e.message}")
        Toast.makeText(
            context,
            "Nije moguće promeniti lokaciju. Proverite da li je Loca Edit aplikacija instalirana.",
            Toast.LENGTH_LONG
        ).show()
    }
}

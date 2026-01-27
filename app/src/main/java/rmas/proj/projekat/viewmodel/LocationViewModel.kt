package rmas.proj.projekat.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rmas.proj.projekat.data.Location
import rmas.proj.projekat.data.LocationRepository
import rmas.proj.projekat.data.LocationType
import rmas.proj.projekat.data.Rating
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import rmas.proj.projekat.data.Animal
import rmas.proj.projekat.data.AnimalRepository
import rmas.proj.projekat.data.Fish
import rmas.proj.projekat.data.FishRepository
import kotlinx.coroutines.flow.first


class LocationViewModel(// ViewModel za dodavanje objekata, ocena i filtriranje obj
    private val locationRepository: LocationRepository = LocationRepository(),
    private val animalRepository: AnimalRepository= AnimalRepository(),
    private val fishRepository: FishRepository=FishRepository()
) : ViewModel() {

    private val _isFilteringActive = MutableStateFlow(false)
    val isFilteringActive = _isFilteringActive.asStateFlow()

    private val _locations = MutableStateFlow<Map<String, Location>>(emptyMap())
    val locations = _locations.asStateFlow()

    private val _filteredLocations = MutableStateFlow<List<Location>>(emptyList())
    val filteredLocations = _filteredLocations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _animalsAtLocation = MutableStateFlow<List<Animal>>(emptyList())
    val animalsAtLocation: StateFlow<List<Animal>> = _animalsAtLocation
    private val _fishAtLocation = MutableStateFlow<List<Fish>>(emptyList())
    val fishAtLocation: StateFlow<List<Fish>> = _fishAtLocation

    private val _availableAnimals = MutableStateFlow<List<Animal>>(emptyList())
    val availableAnimals: StateFlow<List<Animal>> = _availableAnimals
    private val _availableFish = MutableStateFlow<List<Fish>>(emptyList())
    val availableFish: StateFlow<List<Fish>> = _availableFish

    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails: StateFlow<Boolean> = _isLoadingDetails
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _selectedAnimals = MutableStateFlow<List<Animal>>(emptyList())
    val selectedAnimals: StateFlow<List<Animal>> = _selectedAnimals.asStateFlow()

    private val _selectedFishes = MutableStateFlow<List<Fish>>(emptyList())
    val selectedFishes: StateFlow<List<Fish>> = _selectedFishes.asStateFlow()

    private var currentSearchQuery = ""
    private var currentType: LocationType? = null
    private var currentAuthorId: String? = null

    fun loadLocation(locationId: String) {
        viewModelScope.launch {
            // ucitava lokaciju
            locationRepository.getLocation(locationId).onSuccess { location ->
                _currentLocation.value = location

                // ucitava zivotinje/ribe
                when (location.type) {
                    LocationType.HUNTING -> {
                        loadAnimalsForLocation(locationId)
                        loadAvailableAnimals(location.animalIds)
                    }
                    LocationType.FISHING -> {
                        loadFishForLocation(locationId)
                        loadAvailableFish(location.fishIds)
                    }
                }
            }
        }
    }

    //------------------------------------------------------------------------------
    //ovo je za pregled zivotinja i riba na lokaicji i dodavanje na lokaciju

    private suspend fun loadAvailableAnimals(excludeIds: List<String>) {
        try {
            Log.d("LocationViewModel", "Loading available animals, excluding: ${excludeIds.size}")
            animalRepository.getAllAnimals()
                .onSuccess { allAnimals ->
                    _availableAnimals.value = allAnimals.filter { it.id !in excludeIds }
                    Log.d("LocationViewModel", "Loaded ${_availableAnimals.value.size} available animals")
                }
                .onFailure { exception ->
                    Log.e("LocationViewModel", "Error loading available animals", exception)
                    _availableAnimals.value = emptyList()
                }
        } catch (e: Exception) {
            Log.e("LocationViewModel", "Exception in loadAvailableAnimals", e)
            _availableAnimals.value = emptyList()
        }
    }

    private suspend fun loadAvailableFish(excludeIds: List<String>) {
        try {
            Log.d("LocationViewModel", "Loading available fish, excluding: ${excludeIds.size}")
            fishRepository.getAllFishes()
                .onSuccess { allFish ->
                    _availableFish.value = allFish.filter { it.id !in excludeIds }
                    Log.d("LocationViewModel", "Loaded ${_availableFish.value.size} available fish")
                }
                .onFailure { exception ->
                    Log.e("LocationViewModel", "Error loading available fish", exception)
                    _availableFish.value = emptyList()
                }
        } catch (e: Exception) {
            Log.e("LocationViewModel", "Exception in loadAvailableFish", e)
            _availableFish.value = emptyList()
        }
    }


    fun loadLocationDetails(locationId: String) {
        viewModelScope.launch {
            _isLoadingDetails.value = true
            _error.value = null

            try {
                Log.d("LocationViewModel", "Loading details for location: $locationId")
                // Učitaj lokaciju
                locationRepository.getLocation(locationId)
                    .onSuccess { location ->
                        _currentLocation.value = location

                        // Učitaj životinje ili ribe zavisno od tipa
                        when (location.type) {
                            LocationType.HUNTING -> {
                                loadAnimalsForLocation(locationId)
                                loadAvailableAnimals(location.animalIds)
                            }
                            LocationType.FISHING -> {
                                loadFishForLocation(locationId)
                                loadAvailableFish(location.fishIds)
                            }
                        }
                        Log.d("LocationViewModel", "Successfully loaded location details for ${location.name}")
                    }
                    .onFailure { exception ->
                        _error.value = "Error loading location details: ${exception.message}"
                        Log.e("LocationViewModel", "Error loading location details", exception)
                    }
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                Log.e("LocationViewModel", "Unexpected error", e)
            } finally {
                _isLoadingDetails.value = false
            }
        }
    }


    private suspend fun loadAnimalsForLocation(locationId: String) {
        try {
            Log.d("LocationViewModel", "Fetching animals for location: $locationId")
            locationRepository.getAnimalsForLocation(locationId)
                .onSuccess { animals ->
                    _animalsAtLocation.value = animals
                    Log.d("LocationViewModel", "Loaded ${animals.size} animals for location")
                }
                .onFailure { exception ->
                    Log.e("LocationViewModel", "Error loading animals for location", exception)
                    _animalsAtLocation.value = emptyList()
                }
        } catch (e: Exception) {
            Log.e("LocationViewModel", "Exception in loadAnimalsForLocation", e)
            _animalsAtLocation.value = emptyList()
        }
    }

    private suspend fun loadFishForLocation(locationId: String) {
        try {
            Log.d("LocationViewModel", "Fetching fish for location: $locationId")
            locationRepository.getFishForLocation(locationId)
                .onSuccess { fish ->
                    _fishAtLocation.value = fish
                    Log.d("LocationViewModel", "Loaded ${fish.size} fish for location")
                }
                .onFailure { exception ->
                    Log.e("LocationViewModel", "Error loading fish for location", exception)
                    _fishAtLocation.value = emptyList()
                }
        } catch (e: Exception) {
            Log.e("LocationViewModel", "Exception in loadFishForLocation", e)
            _fishAtLocation.value = emptyList()
        }
    }

    fun addAnimalToLocation(locationId: String, animalId: String) {
        viewModelScope.launch {
            _isLoadingDetails.value = true

            locationRepository.addAnimalToLocation(locationId, animalId)
                .onSuccess {
                    loadLocationDetails(locationId) // Refresh detalja
                    Log.d("LocationViewModel", "Successfully added animal to location")
                }
                .onFailure { exception ->
                    _error.value = "Error adding animal: ${exception.message}"
                    Log.e("LocationViewModel", "Error adding animal to location", exception)
                }

            _isLoadingDetails.value = false
        }
    }

    fun addFishToLocation(locationId: String, fishId: String) {
        viewModelScope.launch {
            _isLoadingDetails.value = true

            locationRepository.addFishToLocation(locationId, fishId)
                .onSuccess {
                    loadLocationDetails(locationId) // Refresh detalja
                    Log.d("LocationViewModel", "Successfully added fish to location")
                }
                .onFailure { exception ->
                    _error.value = "Error adding fish: ${exception.message}"
                    Log.e("LocationViewModel", "Error adding fish to location", exception)
                }

            _isLoadingDetails.value = false
        }
    }

    fun removeAnimalFromLocation(locationId: String, animalId: String) {
        viewModelScope.launch {
            _isLoadingDetails.value = true

            locationRepository.removeAnimalFromLocation(locationId, animalId)
                .onSuccess {
                    loadLocationDetails(locationId) // Refresh detalja
                    Log.d("LocationViewModel", "Successfully removed animal from location")
                }
                .onFailure { exception ->
                    _error.value = "Error removing animal: ${exception.message}"
                    Log.e("LocationViewModel", "Error removing animal from location", exception)
                }

            _isLoadingDetails.value = false
        }
    }

    fun removeFishFromLocation(locationId: String, fishId: String) {
        viewModelScope.launch {
            _isLoadingDetails.value = true

            locationRepository.removeFishFromLocation(locationId, fishId)
                .onSuccess {
                    loadLocationDetails(locationId) // Refresh detalja
                    Log.d("LocationViewModel", "Successfully removed fish from location")
                }
                .onFailure { exception ->
                    _error.value = "Error removing fish: ${exception.message}"
                    Log.e("LocationViewModel", "Error removing fish from location", exception)
                }

            _isLoadingDetails.value = false
        }
    }

    //-------------------------------------------------------------------------------------------
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

    //-------------------------------------------------------------------------------------------
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri = _selectedImageUri.asStateFlow()

    fun updateSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    init {
        loadAllLocations()
    }

    //-------------------FILTER---------------------------------------------------------------------


    fun filterLocationsByAnimals(selectedAnimalIds: Set<String>) {
        viewModelScope.launch {
            _isFilteringActive.value = true
            _isLoading.value = true

            try {
                viewModelScope.launch{
                    if (selectedAnimalIds.isEmpty()) {// Ako nema selektovanih životinja, prikaži sve lokacije

                        _filteredLocations.value = _locations.value.values.toList()
                        _isFilteringActive.value = false
                    } else {
                        // Filtriraj lokacije koje sadrže SVE selektovane životinje
                        val filtered = _locations.value.values.filter { location ->
                            // Proveri da li lokacija sadrži SVE selektovane životinje
                            selectedAnimalIds.all { animalId ->
                                location.animalIds.contains(animalId)
                            }
                        }
                        _filteredLocations.value = filtered
                        _isFilteringActive.value = true

                        Log.d(
                            "LocationViewModel",
                            "Filtered ${filtered.size} locations with all selected animals"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationViewModel", "Error filtering locations", e)
                _error.value = "Greška pri filtriranju: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterLocationsByFieshes(selectedFishIds: Set<String>) {
        viewModelScope.launch {
            _isFilteringActive.value = true
            _isLoading.value = true

            try {
                viewModelScope.launch{
                    if (selectedFishIds.isEmpty()) {// Ako nema selektovanih životinja, prikaži sve lokacije
                        _filteredLocations.value = _locations.value.values.toList()
                        _isFilteringActive.value = false
                    } else {
                        // Filtriraj lokacije koje sadrže SVE selektovane ribe
                        val filtered = _locations.value.values.filter { location ->
                            // Proveri da li lokacija sadrži SVE selektovane ribe
                            selectedFishIds.all { fishId ->
                                location.fishIds.contains(fishId)
                            }
                        }
                        _filteredLocations.value = filtered
                        _isFilteringActive.value = true

                        Log.d(
                            "LocationViewModel",
                            "Filtered ${filtered.size} locations with all selected fishes"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationViewModel", "Error filtering locations", e)
                _error.value = "Greška pri filtriranju: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ViewModel deo
    fun toggleAnimalSelection(animal: Animal) {
        val currentSelected = _selectedAnimals.value.toMutableList()
        if (currentSelected.any { it.id == animal.id }) {
            currentSelected.removeAll { it.id == animal.id }
        } else {
            currentSelected.add(animal)
        }
        _selectedAnimals.value = currentSelected

        // Automatski ažuriraj filtere čim se nešto promeni
        applyFilters()
        _isFilteringActive.value = _selectedAnimals.value.isNotEmpty() || _selectedFishes.value.isNotEmpty()
    }

    fun toggleFishSelection(fish: Fish) {
        val currentSelected = _selectedFishes.value.toMutableList()
        if (currentSelected.any { it.id == fish.id }) {
            currentSelected.removeAll { it.id == fish.id }
        } else {
            currentSelected.add(fish)
        }
        _selectedFishes.value = currentSelected

        // Automatski ažuriraj filtere čim se nešto promeni
        applyFilters()
        _isFilteringActive.value = _selectedAnimals.value.isNotEmpty() || _selectedFishes.value.isNotEmpty()
    }


    private fun loadAllLocations() {
        viewModelScope.launch {
            locationRepository.getAllLocations()
                .onSuccess { locationsList ->
                    _locations.value = locationsList.associateBy { it.id }

                    // Ako je filter vec aktivan, ne diraj filteredLocations direktno nego pozovi applyFilters da ponovo procesira nove podatke
                    if (_isFilteringActive.value) {
                        applyFilters()
                    } else {
                        _filteredLocations.value = locationsList
                    }
                }
        }
    }

    fun applyFilters(
        authorId: String? = currentAuthorId,
        type: LocationType? = currentType,
        searchQuery: String = currentSearchQuery
    ) {
        currentSearchQuery = searchQuery
        currentType = type
        currentAuthorId = authorId

        val selectedAnimalsIds = _selectedAnimals.value.map { it.id }
        val selectedFishIds = _selectedFishes.value.map { it.id }

        val filtered = _locations.value.values.filter { location ->
            val matchesAuthor = currentAuthorId == null || location.authorId == currentAuthorId
            val matchesType = currentType == null || location.type == currentType
            val matchesSearch = location.name.contains(currentSearchQuery, ignoreCase = true) ||
                    location.description.contains(currentSearchQuery, ignoreCase = true)

            // lokacije koje sadrze sve zivotinje koje su selektovane u filteru
            val matchesAnimals = selectedAnimalsIds.isEmpty() ||
                    selectedAnimalsIds.all { id -> location.animalIds.contains(id) }

            val matchesFish = selectedFishIds.isEmpty() ||
                    selectedFishIds.all { id -> location.fishIds.contains(id) }

            matchesAuthor && matchesType && matchesSearch && matchesAnimals && matchesFish
        }

        _filteredLocations.value = filtered
        if (selectedAnimalsIds.isNotEmpty() || selectedFishIds.isNotEmpty()) {
            _isFilteringActive.value = true
        }
        Log.d("APPLY_FILTERS", "Final Result: ${filtered.size} locations found.")
    }

    private suspend fun ensureLocationsLoaded() {
        if (_locations.value.isEmpty()) {
            _locations.first { it.isNotEmpty() }
        }
    }


    fun clearAllFilters() {
        _selectedAnimals.value = emptyList()
        _selectedFishes.value = emptyList()
        _isFilteringActive.value = false
        currentSearchQuery = ""
        currentType = null
        applyFilters() // Vratiće sve lokacije na mapu
    }


    //-----------------------------------------------------------------------------------------------------------------------------------------

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

    //nova setupLocationUpdates 🤔ne znam da li lepo radi
    fun setupLocationUpdates(
        fusedLocationProvider: FusedLocationProviderClient,
        context: Context
    ) {

        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // NEMA dozvolu → ne radi ništa
            Log.d("LocationVM", "Location permission not granted")
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 5000
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


}

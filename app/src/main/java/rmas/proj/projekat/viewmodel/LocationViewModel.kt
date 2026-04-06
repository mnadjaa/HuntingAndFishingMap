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
//import rmas.proj.projekat.data.AnimalSightingRepository
import kotlinx.coroutines.flow.first
import rmas.proj.projekat.data.SightingRepository
import rmas.proj.projekat.data.SpeciesType


class LocationViewModel(
    private val locationRepository: LocationRepository = LocationRepository(),
    private val animalRepository: AnimalRepository= AnimalRepository(),
    private val fishRepository: FishRepository=FishRepository(),
    private val sightingRepository: SightingRepository = SightingRepository()
    //private val animalSightingRepository: AnimalSightingRepository = AnimalSightingRepository()
) : ViewModel() {
    private val _isFilteringActive = MutableStateFlow(false)
    val isFilteringActive = _isFilteringActive.asStateFlow()
    private val _locations = MutableStateFlow<Map<String, Location>>(emptyMap())
    val locations = _locations.asStateFlow()
    private val _filteredLocations = MutableStateFlow<List<Location>>(emptyList())
    val filteredLocations = _filteredLocations.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
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
    private val _currentLocation = MutableStateFlow<Location?>(null)
    private val _selectedAnimals = MutableStateFlow<List<Animal>>(emptyList())
    val selectedAnimals: StateFlow<List<Animal>> = _selectedAnimals.asStateFlow()
    private val _selectedFishes = MutableStateFlow<List<Fish>>(emptyList())
    val selectedFishes: StateFlow<List<Fish>> = _selectedFishes.asStateFlow()
    private var currentSearchQuery = ""
    private var currentAuthorId: String? = null
    private val _selectedLocationType = MutableStateFlow<LocationType?>(null)
    val selectedLocationType: StateFlow<LocationType?> = _selectedLocationType.asStateFlow()
    private val _selectedMaxDistance = MutableStateFlow<Double?>(null)
    val selectedMaxDistance: StateFlow<Double?> = _selectedMaxDistance.asStateFlow()
    private val _userLocation = MutableStateFlow<LatLng?>(null)

    //📅
    private val _selectedLastSeenPeriod = MutableStateFlow<Long?>(null) // u milisekundama
    val selectedLastSeenPeriod: StateFlow<Long?> = _selectedLastSeenPeriod.asStateFlow()
    fun setLastSeenPeriod(periodMs: Long?) {
        _selectedLastSeenPeriod.value = periodMs
        applyFilters()
    }
    //📅

    //----------------------------zivotinje i ribe--------------------------------------------------
    private val _allSightings = MutableStateFlow<Map<String, Map<String, Long>>>(emptyMap())
    private suspend fun loadAvailableSpecies(
        excludeIds: List<String>,
        type: SpeciesType
    ) {
        try {
            when(type){
                SpeciesType.ANIMAL -> {
                    animalRepository.getAllAnimals()
                        .onSuccess { animals ->
                            _availableAnimals.value =
                                animals.filter { it.id !in excludeIds }
                        }
                }
                SpeciesType.FISH -> {
                    fishRepository.getAllFishes()
                        .onSuccess { fish ->
                            _availableFish.value =
                                fish.filter { it.id !in excludeIds }
                        }
                }
            }
        } catch (e: Exception) {
            Log.e("LocationViewModel", "Error loading species", e)
        }
    }

    //ucitava info o lokaciji
    fun loadLocationDetails(locationId: String) {
        viewModelScope.launch {
            _isLoadingDetails.value = true
            _error.value = null
            try {
                Log.d("LocationViewModel", "Loading details for location: $locationId")
                // ucitaj lokaciju
                locationRepository.getLocation(locationId)
                    .onSuccess { location ->
                        _currentLocation.value = location

                        when (location.type) {
                            LocationType.HUNTING -> {
                                loadSpeciesForLocation(locationId, SpeciesType.ANIMAL)
                                loadAvailableSpecies(location.animalIds, SpeciesType.ANIMAL)
                            }
                            LocationType.FISHING -> {
                                loadSpeciesForLocation(locationId, SpeciesType.FISH)
                                loadAvailableSpecies(location.fishIds, SpeciesType.FISH)
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
    //load forlocation ucitava zivotinje/ribe na lokaciji
    private suspend fun loadSpeciesForLocation(
        locationId: String,
        type: SpeciesType
    ) {
        try {
            Log.d("LocationViewModel", "Fetching $type for location: $locationId")
            val result = locationRepository.getSpeciesForLocation(locationId, type)
            result.onSuccess { speciesList ->
                when (type) {
                    SpeciesType.ANIMAL -> {
                        _animalsAtLocation.value = speciesList.filterIsInstance<Animal>()
                    }
                    SpeciesType.FISH -> {
                        _fishAtLocation.value = speciesList.filterIsInstance<Fish>()
                    }
                }
                Log.d(
                    "LocationViewModel",
                    "Loaded ${speciesList.size} $type for location"
                )
            }.onFailure { e ->
                Log.e("LocationViewModel", "Error loading $type", e)
                when (type) {
                    SpeciesType.ANIMAL -> _animalsAtLocation.value = emptyList()
                    SpeciesType.FISH -> _fishAtLocation.value = emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("LocationViewModel", "Exception loading $type", e)
        }
    }
    //dodavanje ziv/riba na lokaciju
    fun addSpeciesToLocation(
        locationId: String,
        speciesId: String,
        type: SpeciesType
    ) {
        viewModelScope.launch {
            _isLoadingDetails.value = true
            locationRepository
                .addSpeciesToLocation(locationId, speciesId, type)
                .onSuccess {
                    loadLocationDetails(locationId)
                }
                .onFailure {
                    _error.value = it.message
                }
            _isLoadingDetails.value = false
        }
    }
    init {
        loadAllLocations()
    }
    //-----------------pokretni marker--------------------------------------------------------------------------
    private val _characterPosition = MutableStateFlow<LatLng?>(null)
    var characterPosition by mutableStateOf<LatLng?>(null)
        private set
    fun initializeCharacterPosition(position: LatLng) {//pokretni marker 🚮
        characterPosition = position
        _characterPosition.value = position
    }
    fun updatePosition(newPosition: LatLng) {//pokretni marker 🚮
        characterPosition = newPosition
        _characterPosition.value = newPosition
    }
    //------------------image-------------------------------------------------------------------------
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri = _selectedImageUri.asStateFlow()
    fun updateSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }
    //-------------------FILTRIRANJE---------------------------------------------------------------------
    //za selektovanje i deselektovanje zivotinja/riba
    fun toggleAnimalSelection(animal: Animal) {
        val currentSelected = _selectedAnimals.value.toMutableList()
        if (currentSelected.any { it.id == animal.id }) {
            currentSelected.removeAll { it.id == animal.id }
        } else {
            currentSelected.add(animal)
        }
        _selectedAnimals.value = currentSelected

        // automatski azuriraj filtere cim se nesto promeni
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

    //za odabir lov/ribolov
    fun setLocationType(type: LocationType?) {
        _selectedLocationType.value = type
        _isFilteringActive.value=true
        Log.d("FILTER_DEBUG", "SET TYPE = $type")
        applyFilters()
    }

    //za udaljenost
    fun setUserLocation(latLng: LatLng) {
        _userLocation.value = latLng
    }

    fun setMaxDistance(distanceKm: Double?) {
        _selectedMaxDistance.value = distanceKm
        applyFilters()
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000.0
    }

    private fun loadAllLocations() {
        viewModelScope.launch {
            locationRepository.getAllLocations()
                .onSuccess { locationsList ->
                    _locations.value = locationsList.associateBy { it.id }
                    applyFilters()
                }
        }
    }

    fun applyFilters(
        authorId: String? = currentAuthorId,
        searchQuery: String = currentSearchQuery
    ) {
        currentSearchQuery = searchQuery
        currentAuthorId = authorId
        val type = _selectedLocationType.value
        val selectedAnimalsIds = _selectedAnimals.value.map { it.id }
        val selectedFishIds = _selectedFishes.value.map { it.id }

        val filtered = _locations.value.values.filter { location ->
            val matchesAuthor = currentAuthorId == null || location.authorId == currentAuthorId
            val matchesType = when (type) {
                null -> true
                else -> location.type == type
            }
            val matchesSearch = location.name.contains(currentSearchQuery, ignoreCase = true) ||
                    location.description.contains(currentSearchQuery, ignoreCase = true)

            val matchesAnimalsOrFish = when {
                selectedAnimalsIds.isEmpty() && selectedFishIds.isEmpty() -> true
                selectedAnimalsIds.isNotEmpty() && selectedFishIds.isNotEmpty() ->
                    selectedAnimalsIds.any { id -> location.animalIds.contains(id) } ||
                            selectedFishIds.any { id -> location.fishIds.contains(id) }
                selectedAnimalsIds.isNotEmpty() ->
                    selectedAnimalsIds.any { id -> location.animalIds.contains(id) }
                else ->
                    selectedFishIds.any { id -> location.fishIds.contains(id) }
            }

            val userLoc = _userLocation.value
            val maxDist = _selectedMaxDistance.value

            val matchesDistance = if (maxDist != null && userLoc != null) {
                val dist = distanceBetween(
                    userLoc.latitude, userLoc.longitude,
                    location.position.latitude, location.position.longitude
                )
                dist <= maxDist
            } else true
            //deo za last seen: 📅
            val lastSeenPeriod = _selectedLastSeenPeriod.value
            val now = System.currentTimeMillis()

            val matchesLastSeen = if (lastSeenPeriod != null &&
                (selectedAnimalsIds.isNotEmpty() || selectedFishIds.isNotEmpty())) {

                val sightingsForLocation = _allSightings.value[location.id] ?: emptyMap()
                val targetIds = selectedAnimalsIds + selectedFishIds

                // Da li je IJEDNA od selektovanih životiinja/riba viđena u periodu
                targetIds.any { id ->
                    val lastSeen = sightingsForLocation[id]
                    lastSeen != null && (now - lastSeen) <= lastSeenPeriod
                }
            } else if (lastSeenPeriod != null) {
                // Nijedna životinja nije selektovana — traži lokacije gde je BILO KOJA viđena u periodu
                val sightingsForLocation = _allSightings.value[location.id] ?: emptyMap()
                sightingsForLocation.values.any { lastSeen ->
                    (now - lastSeen) <= lastSeenPeriod
                }
            } else true //📅

            matchesAuthor && matchesType && matchesSearch && matchesAnimalsOrFish && matchesDistance && matchesLastSeen
        }

        _filteredLocations.value = filtered
        if (selectedAnimalsIds.isNotEmpty() || selectedFishIds.isNotEmpty() || type != null || currentAuthorId != null ||  _selectedMaxDistance.value != null)
        {
            _isFilteringActive.value = true
        }
        Log.d("APPLY_FILTERS", "Final Result: ${filtered.size} locations found.")
        Log.d("FILTER_DEBUG", "TYPE = $type")
        Log.d("FILTER_DEBUG", "ALL LOCATIONS = ${_locations.value.values.map { it.type }}")
    }
    fun clearAllFilters() {
        _selectedAnimals.value = emptyList()
        _selectedFishes.value = emptyList()
        _selectedLocationType.value = null
        currentSearchQuery = ""
        _selectedMaxDistance.value = null
        _selectedLastSeenPeriod.value = null
        _isFilteringActive.value = false
        applyFilters() // vratice sve lokacije na mapu nakon sto obrise filtere
    }
    //----------------------------------ovo sve brisem 🚮-------------------------------------------------------------------------------------------------------
    fun clearError() {
        _error.value = null
    }
    // 📌
    private var locationUpdateActive = false
    lateinit var locationCallback: LocationCallback

    /*
    private val _isManuallyControlling = MutableStateFlow(false)
    val isManuallyControlling: StateFlow<Boolean> = _isManuallyControlling.asStateFlow()
    fun setManualControl(isManual: Boolean) {
        _isManuallyControlling.value = isManual
    }*/
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
            // ako nema dozvolu  ne radi nista
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
    //-----------------------------------add i update-------------------------------------------------------------
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

   /* fun deleteLocation(locationId: String) {
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
    }*/

    //-----------------rating--------------------------------------------------------------------------------
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

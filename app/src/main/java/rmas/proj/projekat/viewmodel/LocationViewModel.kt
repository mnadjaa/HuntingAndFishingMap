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
import com.google.firebase.auth.FirebaseAuth
import rmas.proj.projekat.data.Animal
import rmas.proj.projekat.data.AnimalRepository
import rmas.proj.projekat.data.Fish
import rmas.proj.projekat.data.FishRepository
import rmas.proj.projekat.data.SightingRepository
import rmas.proj.projekat.data.SpeciesType
import rmas.proj.projekat.data.UserRepository

class LocationViewModel(
    private val locationRepository: LocationRepository = LocationRepository(),
    private val animalRepository: AnimalRepository= AnimalRepository(),
    private val fishRepository: FishRepository=FishRepository(),
    private val sightingRepository: SightingRepository = SightingRepository(),
    private val userRepository: UserRepository = UserRepository()
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

    private val _selectedAuthorId = MutableStateFlow<String?>(null)
    val selectedAuthorId = _selectedAuthorId.asStateFlow()
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
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            locationRepository
                .addSpeciesToLocation(locationId, speciesId, type)
                .onSuccess {
                    userRepository.addPoints(currentUserId, 2)
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
        loadAllSightings()
    }
    //-----------------nas marker (msm da mogu da brisem jer nemam vise pokretni marker)--------------------------------------------------------------------------
    private val _characterPosition = MutableStateFlow<LatLng?>(null)
    var characterPosition by mutableStateOf<LatLng?>(null)
        private set
    fun initializeCharacterPosition(position: LatLng) {
        characterPosition = position
        _characterPosition.value = position
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

        // automatski azuriraj filtere cim se nesto selektuje ili deselektuje
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

        // automatski se auzuriraju filteri ako se nesto selektuje
        applyFilters()
        _isFilteringActive.value = _selectedAnimals.value.isNotEmpty() || _selectedFishes.value.isNotEmpty()
    }

    fun toggleAuthorSelection(userId: String) {
        val newAuthorId = if (_selectedAuthorId.value == userId) {
            null // deselect ako je isti
        } else {
            userId // select novi
        }

        _selectedAuthorId.value = newAuthorId

        _isFilteringActive.value =
            newAuthorId != null ||
                    _selectedAnimals.value.isNotEmpty() ||
                    _selectedFishes.value.isNotEmpty() ||
                    _selectedLocationType.value != null ||
                    _selectedMaxDistance.value != null ||
                    _selectedLastSeenPeriod.value != null

        applyFilters()
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
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results) //racuna krivu na zem kugli
        return results[0] / 1000.0//results je u metrima - vracamo km
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
    private fun loadAllSightings() {
        viewModelScope.launch {
            sightingRepository.getSightingsFlow()
                .collect { sightingsList ->
                    val mapped = sightingsList
                        .groupBy { it.locationId }
                        .mapValues { (_, sightingsAtLocation) ->
                            sightingsAtLocation.associate {
                                it.speciesId to it.lastSeen
                            }
                        }
                    _allSightings.value = mapped
                    applyFilters()
                }
        }
    }
    private val _selectedLastSeenPeriod = MutableStateFlow<Long?>(null)
    val selectedLastSeenPeriod: StateFlow<Long?> = _selectedLastSeenPeriod.asStateFlow()

    fun setLastSeenPeriod(periodMs: Long?) {
        _selectedLastSeenPeriod.value = periodMs
        _isFilteringActive.value = true
        applyFilters()
    }

    fun applyFilters(
        searchQuery: String = currentSearchQuery
    ) {
        currentSearchQuery = searchQuery
        val authorId = _selectedAuthorId.value
        val type = _selectedLocationType.value
        val selectedAnimalsIds = _selectedAnimals.value.map { it.id }
        val selectedFishIds = _selectedFishes.value.map { it.id }
        val now = System.currentTimeMillis()
        val lastSeenPeriod = _selectedLastSeenPeriod.value


        val filtered = _locations.value.values.filter { location ->
            val matchesAuthor = authorId == null || location.authorId == authorId
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

            val sightingsForLocation = _allSightings.value[location.id] ?: emptyMap()
            // - ako je selektovana neka vrsta mora neka od njih da bude vidjena u tom periodu a ako nisu onda bilo koja
            val matchesLastSeen = if (lastSeenPeriod == null) {
                true
            } else {
                val cutoff = now - lastSeenPeriod
                Log.d("LASTSEEN_DEBUG", "now=$now, cutoff=$cutoff, period=$lastSeenPeriod")

                if (selectedAnimalsIds.isEmpty() && selectedFishIds.isEmpty()) {
                    val sightingValues = sightingsForLocation.values.toList()
                    Log.d("LASTSEEN_DEBUG", "Location ${location.name}: sightings=$sightingValues")
                    sightingsForLocation.values.any { lastSeen -> lastSeen >= cutoff }
                } else {
                    // postoje selektovane vrste i samo se one proveravaju
                    val relevantIds = selectedAnimalsIds + selectedFishIds
                    relevantIds.any { id ->
                        val lastSeen = sightingsForLocation[id] ?: 0L
                        Log.d("LASTSEEN_DEBUG", "Location ${location.name}, speciesId=$id, lastSeen=$lastSeen, cutoff=$cutoff, passes=${lastSeen >= cutoff}")
                        lastSeen >= cutoff
                    }
                }
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
        _selectedAuthorId.value = null
        currentSearchQuery = ""
        _selectedMaxDistance.value = null
        _isFilteringActive.value = false
        _selectedLastSeenPeriod.value = null
        applyFilters() // vratice sve lokacije na mapu nakon sto obrise filtere
    }


    //----------------------------------ovo sve brisem -------------------------------------------------------------------------------------------------------
    fun clearError() {
        _error.value = null
    }
    private var locationUpdateActive = false
    lateinit var locationCallback: LocationCallback
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
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    userRepository.addPoints(currentUserId, 5)
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

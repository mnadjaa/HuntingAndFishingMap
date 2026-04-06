package rmas.proj.projekat.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rmas.proj.projekat.data.Animal
import rmas.proj.projekat.data.AnimalRepository
import rmas.proj.projekat.data.Fish
import rmas.proj.projekat.data.FishRepository
import rmas.proj.projekat.data.SightingRepository
import rmas.proj.projekat.data.Species
import rmas.proj.projekat.data.SpeciesType

class SpeciesViewModel(
    private val animalRepository: AnimalRepository = AnimalRepository(),
    private val fishRepository: FishRepository = FishRepository(),
    private val sightingRepository: SightingRepository = SightingRepository()
) : ViewModel() {
    private val _locationSightings = MutableStateFlow<Map<String, Long>>(emptyMap())
    val locationSightings: StateFlow<Map<String, Long>> = _locationSightings.asStateFlow()

    private val _animalsData = MutableStateFlow<Map<String, Animal>>(emptyMap())
    val animalsData: StateFlow<Map<String, Animal>> = _animalsData.asStateFlow()

    private val _fishData = MutableStateFlow<Map<String, Fish>>(emptyMap())
    val fishData: StateFlow<Map<String, Fish>> = _fishData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadAllSpecies(type: SpeciesType) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                when (type) {
                    SpeciesType.ANIMAL -> {
                        animalRepository.getAllAnimals()
                            .onSuccess { list ->
                                _animalsData.value = list.associateBy { it.id }
                            }
                            .onFailure { _error.value = it.message }
                    }
                    SpeciesType.FISH -> {
                        fishRepository.getAllFishes()
                            .onSuccess { list ->
                                _fishData.value = list.associateBy { it.id }
                            }
                            .onFailure { _error.value = it.message }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLastSeen(
        speciesId: String,
        locationId: String
    ) {
        viewModelScope.launch {
            sightingRepository.updateLastSeen(
                speciesId,
                locationId
            )
        }
    }

    fun loadSightingsForLocation(locationId: String)
    {
        viewModelScope.launch {
            sightingRepository
                .getSightingsForLocation(locationId)
                .onSuccess { sightings ->
                    val map = sightings.associate {
                        it.speciesId to it.lastSeen
                    }
                    _locationSightings.value = map
                }
        }
    }
}
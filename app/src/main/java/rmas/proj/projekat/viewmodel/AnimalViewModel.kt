package rmas.proj.projekat.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rmas.proj.projekat.data.Animal
import rmas.proj.projekat.data.AnimalRepository
import rmas.proj.projekat.data.AnimalSightingRepository

class AnimalViewModel(
    private val animalRepository: AnimalRepository = AnimalRepository(),
    private val animalSightingRepository: AnimalSightingRepository = AnimalSightingRepository()
) : ViewModel() {

    private val _animalsData = MutableStateFlow<Map<String, Animal>>(emptyMap())
    val animalsData: StateFlow<Map<String, Animal>> = _animalsData.asStateFlow()

    //selected image mi ne treba jer nece korisnici da dodaju slike zivotinja
//    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
//    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _locationSightings = MutableStateFlow<Map<String, Long>>(emptyMap())
    val locationSightings: StateFlow<Map<String, Long>> = _locationSightings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadAllAnimals() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            animalRepository.getAllAnimals()
                .onSuccess { animals ->
                    val animalMap = animals.associateBy { it.id }
                    _animalsData.value = animalMap
                    Log.d("AnimalViewModel", "Uspešno učitane sve životinje: ${animals.size}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju životinja: ${exception.message}"
                    Log.e("AnimalViewModel", "Greška pri učitavanju životinja", exception)
                }

            _isLoading.value = false
        }
    }
    fun updateLastSeen(animalId: String, locationId:String) {
        viewModelScope.launch {
            animalSightingRepository.updateLastSeen(animalId,locationId)
        }
    }
    fun loadSightingsForLocation(locationId: String) {
        viewModelScope.launch {

            animalSightingRepository.getSightingsForLocation(locationId)
                .onSuccess { sightings ->

                    val map = sightings.associate {
                        it.animalId to it.lastSeen
                    }

                    _locationSightings.value = map
                }

                .onFailure {
                    Log.e("AnimalViewModel", "Error loading sightings", it)
                }
        }
    }

}


    /*fun loadAnimalData(animalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            animalRepository.getAnimalById(animalId)
                .onSuccess { animal ->
                    _animalsData.value = _animalsData.value + (animalId to animal)
                    Log.d("AnimalViewModel", "Uspešno učitana životinja: ${animal.name}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju životinje: ${exception.message}"
                    Log.e("AnimalViewModel", "Greška pri učitavanju životinje", exception)
                }

            _isLoading.value = false
        }
    }*/

/*    fun updateSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }
*/


    /*fun loadAnimalsByAuthor(authorId: String) { //ovo nece da mi treba
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            animalRepository.getAnimalsByAuthor(authorId)
                .onSuccess { animals ->
                    val animalMap = animals.associateBy { it.id }
                    _animalsData.value = _animalsData.value + animalMap
                    Log.d("AnimalViewModel", "Uspešno učitane životinje autora: ${animals.size}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju životinja: ${exception.message}"
                    Log.e("AnimalViewModel", "Greška pri učitavanju životinja", exception)
                }

            _isLoading.value = false
        }
    }*/

    /*fun createAnimal(animal: Animal, context: Context) {//, context:Context
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            animalRepository.createAnimal(animal, context, _selectedImageUri.value)//dodala sam , context, _selectedImageUri.value
                .onSuccess { createdAnimal ->
                    _animalsData.value = _animalsData.value + (createdAnimal.id to createdAnimal)
                    Log.d("AnimalViewModel", "Uspešno kreirana životinja: ${createdAnimal.id}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri kreiranju životinje: ${exception.message}"
                    Log.e("AnimalViewModel", "Greška pri kreiranju životinje", exception)
                }

            _isLoading.value = false
        }
    }*/

    /*
    fun updateAnimal(animal: Animal) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            animalRepository.updateAnimal(animal)
                .onSuccess { updatedAnimal ->
                    _animalsData.value = _animalsData.value + (updatedAnimal.id to updatedAnimal)
                    Log.d("AnimalViewModel", "Uspešno ažurirana životinja: ${updatedAnimal.id}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri ažuriranju životinje: ${exception.message}"
                    Log.e("AnimalViewModel", "Greška pri ažuriranju životinje", exception)
                }

            _isLoading.value = false
        }
    }*/

    /*
    fun deleteAnimal(animalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            animalRepository.deleteAnimal(animalId)
                .onSuccess {
                    _animalsData.value = _animalsData.value - animalId
                    Log.d("AnimalViewModel", "Uspešno obrisana životinja: $animalId")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri brisanju životinje: ${exception.message}"
                    Log.e("AnimalViewModel", "Greška pri brisanju životinje", exception)
                }

            _isLoading.value = false
        }
    }*/

    /*fun clearError() {
        _error.value = null
    }*/

    /*fun selectImage(context: Context) {
        val pickImageLauncher =
            (context as? ComponentActivity)?.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { updateSelectedImage(it) }
            }
        pickImageLauncher?.launch("image/*")
    }*/
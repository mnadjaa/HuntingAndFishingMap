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
import rmas.proj.projekat.data.Fish
import rmas.proj.projekat.data.FishRepository


class FishViewModel(
    private val fishRepository: FishRepository = FishRepository()
) : ViewModel() {

    private val _fishData = MutableStateFlow<Map<String, Fish>>(emptyMap())
    val fishData: StateFlow<Map<String, Fish>> = _fishData.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun loadFishData(fishId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.getFishById(fishId)
                .onSuccess { fish ->
                    _fishData.value = _fishData.value + (fishId to fish)
                    Log.d("FishViewModel", "Uspešno učitana riba: ${fish.name}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju ribe: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri učitavanju ribe", exception)
                }

            _isLoading.value = false
        }
    }

    fun loadFishesByAuthor(authorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.getFishesByAuthor(authorId)
                .onSuccess { fishes ->
                    val fishMap = fishes.associateBy { it.id }
                    _fishData.value = _fishData.value + fishMap
                    Log.d("FishViewModel", "Uspešno učitane ribe autora: ${fishes.size}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju riba: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri učitavanju riba", exception)
                }

            _isLoading.value = false
        }
    }

    fun loadAllFishes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.getAllFishes()
                .onSuccess { fishes ->
                    val fishMap = fishes.associateBy { it.id }
                    _fishData.value = fishMap
                    Log.d("FishViewModel", "Uspešno učitane sve ribe: ${fishes.size}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju riba: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri učitavanju riba", exception)
                }

            _isLoading.value = false
        }
    }

    fun createFish(fish: Fish, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.createFish(fish, context, _selectedImageUri.value)
                .onSuccess { createdFish ->
                    _fishData.value = _fishData.value + (createdFish.id to createdFish)
                    Log.d("FishViewModel", "Uspešno kreirana riba: ${createdFish.id}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri kreiranju ribe: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri kreiranju ribe", exception)
                }

            _isLoading.value = false
        }
    }

    fun updateFish(fish: Fish) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.updateFish(fish)
                .onSuccess { updatedFish ->
                    _fishData.value = _fishData.value + (updatedFish.id to updatedFish)
                    Log.d("FishViewModel", "Uspešno ažurirana riba: ${updatedFish.id}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri ažuriranju ribe: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri ažuriranju ribe", exception)
                }

            _isLoading.value = false
        }
    }

    fun deleteFish(fishId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            fishRepository.deleteFish(fishId)
                .onSuccess {
                    _fishData.value = _fishData.value - fishId
                    Log.d("FishViewModel", "Uspešno obrisana riba: $fishId")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri brisanju ribe: ${exception.message}"
                    Log.e("FishViewModel", "Greška pri brisanju ribe", exception)
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun selectImage(context: Context) {
        val pickImageLauncher =
            (context as? ComponentActivity)?.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { updateSelectedImage(it) }
            }
        pickImageLauncher?.launch("image/*")
    }
}

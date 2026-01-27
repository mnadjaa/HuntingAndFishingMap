package rmas.proj.projekat.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rmas.proj.projekat.data.User
import rmas.proj.projekat.data.UserRepository

class UserViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _usersData = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersData: StateFlow<Map<String, User>> = _usersData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            userRepository.getUserById(userId)
                .onSuccess { user ->
                    _usersData.value = _usersData.value + (userId to user)
                    Log.d("UserViewModel", "Uspešno učitan korisnik: ${user.firstName} ${user.lastName}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri učitavanju korisnika: ${exception.message}"
                    Log.e("UserViewModel", "Greška pri učitavanju korisnika", exception)
                }

            _isLoading.value = false
        }
    }

    fun createUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            userRepository.createUser(user)
                .onSuccess { createdUser ->
                    _usersData.value = _usersData.value + (createdUser.id to createdUser)
                    Log.d("UserViewModel", "Uspešno kreiran korisnik: ${createdUser.id}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri kreiranju korisnika: ${exception.message}"
                    Log.e("UserViewModel", "Greška pri kreiranju korisnika", exception)
                }

            _isLoading.value = false
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            userRepository.updateUser(user)
                .onSuccess { updatedUser ->
                    _usersData.value = _usersData.value + (updatedUser.id to updatedUser)
                    Log.d("UserViewModel", "Uspešno ažuriran korisnik: ${updatedUser.id}")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri ažuriranju korisnika: ${exception.message}"
                    Log.e("UserViewModel", "Greška pri ažuriranju korisnika", exception)
                }

            _isLoading.value = false
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            userRepository.deleteUser(userId)
                .onSuccess {
                    _usersData.value = _usersData.value - userId
                    Log.d("UserViewModel", "Uspešno obrisan korisnik: $userId")
                }
                .onFailure { exception ->
                    _error.value = "Greška pri brisanju korisnika: ${exception.message}"
                    Log.e("UserViewModel", "Greška pri brisanju korisnika", exception)
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

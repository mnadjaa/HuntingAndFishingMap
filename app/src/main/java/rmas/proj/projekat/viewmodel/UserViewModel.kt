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


    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    fun loadAllUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers()
                .onSuccess { users ->
                    _allUsers.value = users.sortedByDescending { it.points }
                }
                .onFailure {
                    Log.e("UserViewModel", "Greška pri učitavanju korisnika", it)
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}


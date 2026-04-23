package rmas.proj.projekat.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import rmas.proj.projekat.viewmodel.UserViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()//za asinhrone operacije - zbog firebase poziva

    val userViewModel: UserViewModel = viewModel()
    val usersData by userViewModel.usersData.collectAsState()

    // Prati podatke trenutno prijavljenog korisnika
    //val currentUserId = auth.currentUser?.uid
    //val currentUser = currentUserId?.let { usersData[it] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Korisničko ime (Email)") },
            isError = isError && username.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Lozinka") },
            visualTransformation = PasswordVisualTransformation(),
            isError = isError && password.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        if (isError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (username.isEmpty() || password.isEmpty()) {
                    isError = true
                    errorMessage = "Popunite sva polja"
                    return@Button
                }

                scope.launch {
                    try {
                        auth.signInWithEmailAndPassword(username, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Uspešna prijava",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onLoginSuccess()
                                } else {
                                    isError = true
                                    errorMessage = "Pogrešno korisničko ime ili lozinka"
                                    Toast.makeText(
                                        context,
                                        "Greška pri prijavi: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } catch (e: Exception) {
                        isError = true
                        errorMessage = e.message ?: "Greška pri prijavi"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Prijava")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { onNavigateToRegister() }) {
            Text("Registracija (Kreiraj novi nalog)")
        }
    }
}
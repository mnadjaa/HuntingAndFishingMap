package rmas.proj.projekat.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import rmas.proj.projekat.viewmodel.LocationViewModel
import rmas.proj.projekat.viewmodel.UserViewModel

@Composable
fun AuthorsScreen(
    userViewModel: UserViewModel,
    locationViewModel: LocationViewModel,
    navController: NavController
) {
    val allUsers by userViewModel.allUsers.collectAsState()
    val selectedAuthorId by locationViewModel.selectedAuthorId.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        userViewModel.loadAllUsers()
    }

    // filtriranje liste na osnovu pretrage (ime ili prezime)
    val filteredUsers = allUsers.filter {
        it.firstName.contains(searchQuery, ignoreCase = true) ||
                it.lastName.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Filtriraj po autoru",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Polje za pretragu
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Pretraži autore...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = CircleShape
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredUsers) { user ->
                val isSelected = selectedAuthorId == user.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { locationViewModel.toggleAuthorSelection(user.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slika autora
                        if (user.profilePicture.isNotEmpty()) {
                            AsyncImage(
                                model = user.profilePicture,
                                contentDescription = null,
                                modifier = Modifier.size(45.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(45.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Ime i poeni
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${user.firstName} ${user.lastName}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${user.points} poena",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Indikator selekcije
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selektovan",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
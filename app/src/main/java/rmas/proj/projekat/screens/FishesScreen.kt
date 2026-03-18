package rmas.proj.projekat.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import rmas.proj.projekat.data.Fish
import rmas.proj.projekat.viewmodel.FishViewModel
import rmas.proj.projekat.viewmodel.LocationViewModel
import java.util.UUID


@Composable
fun FishesScreen(
    fishViewModel: FishViewModel,
    locationViewModel: LocationViewModel,
    navController: NavController
) {
    val fishesData by fishViewModel.fishData.collectAsState()
    val isLoading by fishViewModel.isLoading.collectAsState()
    val error by fishViewModel.error.collectAsState()

    val scope = rememberCoroutineScope()
    var isFiltering by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    //var selectedFishes by remember { mutableStateOf(setOf<String>()) }
    val selectedFishesFromVM by locationViewModel.selectedFishes.collectAsState()
    val selectedIds = remember(selectedFishesFromVM) {
        selectedFishesFromVM.map { it.id }.toSet()
    }

    LaunchedEffect(Unit) {
        fishViewModel.loadAllFishes()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Greška pri učitavanju: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                val filteredFishes = fishesData.values.filter{
                    it.name.contains(searchQuery, ignoreCase = true)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Pretraži ribe") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredFishes) { fish ->
                            FishItem(
                                fish = fish,
                                isSelected = selectedIds.contains(fish.id),
                                onToggle = {
                                    locationViewModel.toggleFishSelection(fish)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedFishesFromVM.isNotEmpty() && !isFiltering,
                        onClick = {
                            isFiltering = true
                            navController.navigate("map")
                            isFiltering = false
                        })
                    {
                        Text("Vidi na mapi")
                    }
                    if (selectedFishesFromVM.isNotEmpty()) {
                        TextButton(
                            onClick = { locationViewModel.clearAllFilters() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Očisti selekciju")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FishItem(
    fish: Fish,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable{onToggle()},
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onToggle() }
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.small
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = fish.name, style = MaterialTheme.typography.titleMedium)
                Text(text = fish.description, style = MaterialTheme.typography.bodySmall)
            }
            if (fish.image.isNotEmpty()) {
                AsyncImage(
                    model = fish.image,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

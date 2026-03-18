package rmas.proj.projekat.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import rmas.proj.projekat.data.LocationType
import rmas.proj.projekat.viewmodel.LocationViewModel

@Composable
fun SearchScreen(
    navController: NavController,
    locationViewModel: LocationViewModel
)
{
    val selectedType by locationViewModel.selectedLocationType.collectAsState()
    val selectedMaxDistance by locationViewModel.selectedMaxDistance.collectAsState()
    val distanceOptions = listOf(5.0, 10.0, 25.0, 50.0, 100.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pretraga", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Filter po tipu lokacije",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        //filteri po tipu lokacije:
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == LocationType.HUNTING,
                onClick = {
                    val newType = if (selectedType == LocationType.HUNTING) null else LocationType.HUNTING
                    locationViewModel.setLocationType(newType)
                },
                label = { Text("Lov") },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedType == LocationType.HUNTING) Icons.Default.Check else Icons.Default.Forest,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            FilterChip(
                selected = selectedType == LocationType.FISHING,
                onClick = {
                    val newType = if (selectedType == LocationType.FISHING) null else LocationType.FISHING
                    locationViewModel.setLocationType(newType)
                },
                label = { Text("Ribolov") },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedType == LocationType.FISHING) Icons.Default.Check else Icons.Default.Water,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        //deo za filtriranje po udaljenosti:
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { navController.navigate("animals") }
        ) {
            Icon(Icons.Default.Pets, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Životinje")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { navController.navigate("fishes") }
        ) {
            Icon(Icons.Default.Water, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ribe")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Filter po udaljenosti",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            distanceOptions.forEach { distanceKm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newDist = if (selectedMaxDistance == distanceKm) null else distanceKm
                            locationViewModel.setMaxDistance(newDist)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMaxDistance == distanceKm,
                        onClick = {
                            val newDist = if (selectedMaxDistance == distanceKm) null else distanceKm
                            locationViewModel.setMaxDistance(newDist)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Do ${distanceKm.toInt()} km")
                }
            }
        }
        //kraj

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedType != null || selectedMaxDistance != null ,
            onClick = { navController.navigate("map") }
        ) {
            Text("Vidi na mapi")
        }

        if (selectedType != null || selectedMaxDistance != null) {
            TextButton(
                onClick = { locationViewModel.clearAllFilters() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Očisti filter")
            }
        }
    }
}
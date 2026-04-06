package rmas.proj.projekat.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import rmas.proj.projekat.data.Fish
import rmas.proj.projekat.data.SpeciesType
import rmas.proj.projekat.viewmodel.LocationViewModel
import rmas.proj.projekat.viewmodel.SpeciesViewModel

@Composable
fun FishesScreen(
    speciesViewModel: SpeciesViewModel,
    locationViewModel: LocationViewModel,
    navController: NavController
) {
    val fishData by speciesViewModel.fishData.collectAsState()
    val isLoading by speciesViewModel.isLoading.collectAsState()
    val error by speciesViewModel.error.collectAsState()
    val selectedFishes by locationViewModel.selectedFishes.collectAsState()
    val selectedIds = selectedFishes.map { it.id }.toSet()

    // Ucitava ribe
    LaunchedEffect(Unit) {
        speciesViewModel.loadAllSpecies(SpeciesType.FISH)
    }

    SpeciesScreen(
        data=fishData,
        isLoading = isLoading,
        error = error,
        searchLabel = "Pretraži ribe",
        selectedIds = selectedIds,

        getId = { it.id },
        getName = { it.name },
        getDescription = { it.description },
        getImage = { it.image },

        onToggle = { locationViewModel.toggleFishSelection(it) }
    )
}

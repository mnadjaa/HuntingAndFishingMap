package rmas.proj.projekat.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import rmas.proj.projekat.data.SpeciesType
import rmas.proj.projekat.viewmodel.LocationViewModel
import rmas.proj.projekat.viewmodel.SpeciesViewModel

@Composable
fun AnimalsScreen(
    speciesViewModel: SpeciesViewModel,
    locationViewModel: LocationViewModel,
    navController: NavController//necu da koristim vise obrisi!
) {
    val animalsData by speciesViewModel.animalsData.collectAsState()
    val isLoading by speciesViewModel.isLoading.collectAsState()
    val error by speciesViewModel.error.collectAsState()

    val selectedAnimals by locationViewModel.selectedAnimals.collectAsState()
    val selectedIds = selectedAnimals.map { it.id }.toSet()

    // ucitava zivotinje
    LaunchedEffect(Unit) {
        speciesViewModel.loadAllSpecies(SpeciesType.ANIMAL)
    }

    SpeciesScreen(
        data=animalsData,
        isLoading = isLoading,
        error = error,
        searchLabel = "Pretraži životinje",
        selectedIds = selectedIds,

        getId = { it.id },
        getName = { it.name },
        getDescription = { it.description },
        getImage = { it.image },

        onToggle = { locationViewModel.toggleAnimalSelection(it) }
    )
}

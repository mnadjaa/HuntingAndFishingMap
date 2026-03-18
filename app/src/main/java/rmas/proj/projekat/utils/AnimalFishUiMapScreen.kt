package rmas.proj.projekat.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import rmas.proj.projekat.data.Animal
import rmas.proj.projekat.data.Fish
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.IconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// Dijalog za izbor zivotinje
@Composable
fun AddAnimalToLocationDialog(
    availableAnimals: List<Animal>,
    onDismiss: () -> Unit,
    onAnimalSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Izaberi životinju",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (availableAnimals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sve životinje su već dodate na ovu lokaciju",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(availableAnimals) { animal ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onAnimalSelected(animal.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (animal.image.isNotEmpty()) {
                                        AsyncImage(
                                            model = animal.image,
                                            contentDescription = animal.name,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .padding(end = 12.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = animal.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        /*
                                        Text(
                                            text = animal.species,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )*/
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Otkaži")
                }
            }
        }
    }
}


// Dijalog za izbor ribe
@Composable
fun AddFishToLocationDialog(
    availableFish: List<Fish>,
    onDismiss: () -> Unit,
    onFishSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Izaberi ribu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (availableFish.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sve ribe su već dodate na ovu lokaciju",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(availableFish) { fish ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onFishSelected(fish.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (fish.image.isNotEmpty()) {
                                        AsyncImage(
                                            model = fish.image,
                                            contentDescription = fish.name,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .padding(end = 12.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = fish.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        /*
                                        Text(
                                            text=fish.species,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )*/
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Otkaži")
                }
            }
        }
    }
}


@Composable
fun AnimalsSection(
    animals: List<Animal>,
    sightings: Map<String, Long>,
    onSeenClick: (Animal) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Životinje na ovoj lokaciji",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (expanded)
                    Icons.Default.KeyboardArrowUp
                else
                    Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand"
            )
        }

        if (expanded) {

            if (animals.isEmpty()) {
                Text(
                    text = "Nema dodatih životinja",
                    modifier = Modifier.padding(8.dp)
                )
            } else {

                Column {
                    animals.forEach { animal ->
                        val lastSeen = sightings[animal.id] ?: 0L
                        AnimalRow(
                            animal = animal,
                            lastSeen = lastSeen,
                            onSeenClick = { onSeenClick(animal) }
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun AnimalRow(
    animal: Animal,
    lastSeen: Long,
    onSeenClick: () -> Unit
) {

    var seenClicked by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (animal.image.isNotEmpty()) {
                AsyncImage(
                    model = animal.image,
                    contentDescription = animal.name,
                    modifier = Modifier
                        .size(60.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = animal.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (lastSeen == 0L)
                        "Nije još viđena"
                    else
                        "Poslednji put viđena: ${formatDate(lastSeen)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            IconButton(
                onClick = {
                    seenClicked = !seenClicked
                    if(seenClicked){
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(5000)
                            if (seenClicked) {
                                onSeenClick()
                            }
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Seen",
                    tint = if (seenClicked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


@Composable
fun FishesSection(
    fishes: List<Fish>,
    sightings: Map<String, Long>,
    onSeenClick: (Fish) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Ribe na ovoj lokaciji",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (expanded)
                    Icons.Default.KeyboardArrowUp
                else
                    Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand"
            )
        }

        if (expanded) {

            if (fishes.isEmpty()) {
                Text(
                    text = "Nema dodatih riba",
                    modifier = Modifier.padding(8.dp)
                )
            } else {

                Column {
                    fishes.forEach { fish ->
                        val lastSeen = sightings[fish.id] ?: 0L
                        FishRow(
                            fish = fish,
                            lastSeen = lastSeen,
                            onSeenClick = { onSeenClick(fish) }
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun FishRow(
    fish: Fish,
    lastSeen: Long,
    onSeenClick: () -> Unit
) {

    var seenClicked by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (fish.image.isNotEmpty()) {
                AsyncImage(
                    model = fish.image,
                    contentDescription = fish.name,
                    modifier = Modifier
                        .size(60.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = fish.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (lastSeen == 0L)
                        "Nije još viđena"
                    else
                        "Poslednji put viđena: ${formatDate(lastSeen)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            IconButton(
                onClick = {
                    seenClicked = !seenClicked
                    if(seenClicked){
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(5000)
                            if (seenClicked) {
                                onSeenClick()
                            }
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Seen",
                    tint = if (seenClicked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
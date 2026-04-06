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


// Dijalog za izbor zivotinja i riba
@Composable
fun AddSpeciesToLocationDialog(
    availableSpecies: List<Any>, // Animal ili Fish
    onDismiss: () -> Unit,
    onSpeciesSelected: (String) -> Unit,
    title: String = "Izaberi vrstu"
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (availableSpecies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sve vrste su već dodate na ovu lokaciju",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(availableSpecies) { species ->
                            val name = when (species) {
                                is Animal -> species.name
                                is Fish -> species.name
                                else -> ""
                            }
                            val id = when (species) {
                                is Animal -> species.id
                                is Fish -> species.id
                                else -> ""
                            }
                            val image = when (species) {
                                is Animal -> species.image
                                is Fish -> species.image
                                else -> ""
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSpeciesSelected(id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (image.isNotEmpty()) {
                                        AsyncImage(
                                            model = image,
                                            contentDescription = name,
                                            modifier = Modifier.size(60.dp).padding(end = 12.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Otkaži")
                }
            }
        }
    }
}

@Composable
fun SpeciesSection(
    speciesList: List<Any>,
    sightings: Map<String, Long>,
    title: String,
    onSeenClick: (String) -> Unit // speciesId
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
                text = title,
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
            if (speciesList.isEmpty()) {
                Text(
                    text = "Nema dodatih vrsta",
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                Column {
                    speciesList.forEach { species ->
                        val id = when (species) {
                            is Animal -> species.id
                            is Fish -> species.id
                            else -> ""
                        }
                        val name = when (species) {
                            is Animal -> species.name
                            is Fish -> species.name
                            else -> ""
                        }
                        val image = when (species) {
                            is Animal -> species.image
                            is Fish -> species.image
                            else -> ""
                        }
                        val lastSeen = sightings[id] ?: 0L
                        SpeciesRow(
                            name = name,
                            image = image,
                            lastSeen = lastSeen,
                            onSeenClick = { onSeenClick(id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeciesRow(
    name: String,
    image: String,
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
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (image.isNotEmpty()) {
                AsyncImage(
                    model = image,
                    contentDescription = name,
                    modifier = Modifier.size(60.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold)
                Text(
                    text = if (lastSeen == 0L) "Nije još viđena"
                    else "Poslednji put viđena: ${formatDate(lastSeen)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    seenClicked = !seenClicked
                    if (seenClicked) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(5000)
                            if (seenClicked) onSeenClick()
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Seen",
                    tint = if (seenClicked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

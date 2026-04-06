package rmas.proj.projekat.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width

@Composable
fun <T> SpeciesScreen(
    data: Map<String, T>,
    isLoading: Boolean,
    error: String?,
    searchLabel: String,
    selectedIds: Set<String>,
    getId: (T) -> String,
    getName: (T) -> String,
    getDescription: (T) -> String,
    getImage: (T) -> String,
    onToggle: (T) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Greška pri učitavanju: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                val filtered = data.values.filter {
                    getName(it).contains(searchQuery, ignoreCase = true)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(searchLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {

                        items(filtered) { item ->

                            SpeciesItem(
                                item = item,
                                name = getName,
                                description = getDescription,
                                image = getImage,
                                isSelected = selectedIds.contains(getId(item)),
                                onToggle = { onToggle(item) }
                            )

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SpeciesItem(
    item: T,
    name: (T) -> String,
    description: (T) -> String,
    image: (T) -> String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onToggle() },
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
                    .clickable { onToggle() }
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(name(item), style = MaterialTheme.typography.titleMedium)
                Text(description(item), style = MaterialTheme.typography.bodySmall)
            }

            if (image(item).isNotEmpty()) {
                AsyncImage(
                    model = image(item),
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
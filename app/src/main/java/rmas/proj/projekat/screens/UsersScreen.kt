package rmas.proj.projekat.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import rmas.proj.projekat.data.User
import rmas.proj.projekat.viewmodel.LocationViewModel
import rmas.proj.projekat.viewmodel.UserViewModel

@Composable
fun UsersScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {
    val allUsers by userViewModel.allUsers.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        userViewModel.loadAllUsers()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Rang lista korisnika",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        itemsIndexed(allUsers) { index, user ->
            UserCard(
                user = user,
                rank = index + 1,
                isCurrentUser = user.id == currentUserId
            )
        }
    }
}

@Composable
fun UserCard(
    user: User,
    rank: Int,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // rank
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(45.dp)
            )

            // slika
            if (user.profilePicture.isNotEmpty()) {
                AsyncImage(
                    model = user.profilePicture,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // ime i poeni
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${user.points} poena",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
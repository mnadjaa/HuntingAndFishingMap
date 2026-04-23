package rmas.proj.projekat.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import rmas.proj.projekat.TrackingService

@Composable
fun ServiceControlScreen(context: Context) {            //ukljucivanje i iskljucivanje servisa
    var isServiceRunning by remember { mutableStateOf(TrackingService.isRunning) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isServiceRunning) Icons.Default.LocationOn else Icons.Default.LocationOff,
            contentDescription = null,
            tint = if (isServiceRunning) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isServiceRunning) "Servis je aktivan" else "Servis nije aktivan",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isServiceRunning)
                "Lokacija se šalje periodično i dobijate obaveštenja o mestima za lov/ribolov u blizini."
            else
                "Pokrenite servis da biste pratili lokaciju i dobijali obaveštenja.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val intent = Intent(context, TrackingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isServiceRunning = true
            },
            enabled = !isServiceRunning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pokreni servis")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val intent = Intent(context, TrackingService::class.java)
                context.stopService(intent)
                isServiceRunning = false
            },
            enabled = isServiceRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocationOff, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Zaustavi servis")
        }
    }
}

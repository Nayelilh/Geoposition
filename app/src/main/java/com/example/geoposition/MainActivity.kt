package com.example.nayeliconstantina

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) { LocationScreen() }
    }
}

private data class UiState(
    val lat: Double? = null,
    val lon: Double? = null,
    val accuracyM: Float? = null,
    val lastUpdateMillis: Long? = null,
    val hasPermission: Boolean = false,
    val isLocationOn: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen() {
    val ctx = LocalContext.current

    var hasPermission by remember { mutableStateOf(hasLocationPermission(ctx)) }
    val askPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        hasPermission = (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    var ui by remember {
        mutableStateOf(
            UiState(
                hasPermission = hasPermission,
                isLocationOn = isLocationEnabled(ctx)
            )
        )
    }

    val fused = remember { LocationServices.getFusedLocationProviderClient(ctx) }

    // Escuchar ubicación solo si hay permiso
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            locationFlow(fused).collectLatest { loc ->
                ui = ui.copy(
                    lat = loc.latitude,
                    lon = loc.longitude,
                    accuracyM = loc.accuracy,
                    lastUpdateMillis = System.currentTimeMillis(),
                    hasPermission = true,
                    isLocationOn = isLocationEnabled(ctx)
                )
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Nayeli Constantina") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pedir permisos
            if (!ui.hasPermission) {
                Text("Necesitamos permiso de ubicación para mostrar tus coordenadas.",
                    textAlign = TextAlign.Center)
                Button(onClick = {
                    askPermissions.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }) { Text("Conceder permiso") }
                return@Column
            }

            // Avisar si GPS apagado
            if (!ui.isLocationOn) {
                Text("Tu GPS/Ubicación está desactivado. Enciéndelo para obtener lecturas.",
                    textAlign = TextAlign.Center)
                Button(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Abrir configuración") }
            }

            val latStr = ui.lat?.let { String.format(Locale.US, "%.6f", it) } ?: "—"
            val lonStr = ui.lon?.let { String.format(Locale.US, "%.6f", it) } ?: "—"
            val accStr = ui.accuracyM?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
            val timeStr = ui.lastUpdateMillis?.let {
                SimpleDateFormat("HH:mm:ss 'del' dd-MM-yyyy", Locale.getDefault()).format(Date(it))
            } ?: "—"

            // 📍 Mostrar mapa con la posición
            if (ui.lat != null && ui.lon != null) {
                val position = LatLng(ui.lat!!, ui.lon!!)

                val cameraPositionState = rememberCameraPositionState {
                    this.position = CameraPosition.fromLatLngZoom(position, 16f)
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = position),
                        title = "Nayeli Constantina",
                        snippet = "Precisión: ${ui.accuracyM ?: "?"}m"
                    )
                }
            }




            // Botones extra
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = ui.lat != null && ui.lon != null,
                    onClick = {
                        val lat = ui.lat ?: return@Button
                        val lon = ui.lon ?: return@Button
                        openInGoogleMaps(ctx, lat, lon)
                    }
                ) { Text("Abrir en Google Maps") }

                OutlinedButton(onClick = {
                    ui = ui.copy(
                        isLocationOn = isLocationEnabled(ctx),
                        hasPermission = hasLocationPermission(ctx)
                    )
                    if (!ui.hasPermission) {
                        askPermissions.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                }) { Text("Refrescar estado") }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Tip: el primer fix puede tardar unos segundos; mejor precisión a cielo abierto.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ---------- Helpers ---------- */

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PermissionChecker.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PermissionChecker.PERMISSION_GRANTED
    return fine || coarse
}

private fun isLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gps = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    val net = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
    return gps || net
}

@android.annotation.SuppressLint("MissingPermission")
private fun locationFlow(client: FusedLocationProviderClient)
        = callbackFlow<Location> {
    val request = LocationRequest.Builder(2000L)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMinUpdateIntervalMillis(1000L)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { trySend(it).isSuccess }
        }
    }

    client.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
    client.lastLocation.addOnSuccessListener { it?.let { loc -> trySend(loc).isSuccess } }
    awaitClose { client.removeLocationUpdates(callback) }
}

private fun openInGoogleMaps(context: Context, lat: Double, lon: Double) {
    val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Nayeli%20Constantina)")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) == null) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } else {
        context.startActivity(intent)
    }
}

package com.example.scrollblock

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.KeyboardType
import com.example.scrollblock.internal.IdleScrollAccessibilityService
import com.example.scrollblock.ui.theme.ScrollBlockTheme

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScrollBlockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IdleScrollControlScreen(
                        modifier = Modifier.padding(innerPadding),
                        onOverlayPermissionRequest = { requestOverlayPermission() },
                        onAccessibilityPermissionRequest = { requestAccessibilityPermission() }
                    )
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestAccessibilityPermission() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}

@Composable
fun IdleScrollControlScreen(
    modifier: Modifier = Modifier,
    onOverlayPermissionRequest: () -> Unit,
    onAccessibilityPermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("scroll_block_prefs", Context.MODE_PRIVATE) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isServiceEnabled by remember { mutableStateOf(prefs.getBoolean("detector_enabled", false)) }

    // Advanced Settings States
    var windowMinutes by remember { mutableStateOf(prefs.getLong("pref_window_ms", 300_000L) / 60_000f) }
    var threshold by remember { mutableStateOf(prefs.getInt("pref_threshold", 80).toFloat()) }
    var idleResetSeconds by remember { mutableStateOf(prefs.getLong("pref_idle_reset_ms", 15_000L) / 1_000f) }
    var debounceMs by remember { mutableStateOf(prefs.getLong("pref_debounce_ms", 300L).toFloat()) }
    var overlaySeconds by remember { mutableStateOf(prefs.getInt("pref_overlay_time_s", 3).toFloat()) }

    var showAdvanced by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOverlayGranted = Settings.canDrawOverlays(context)
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
                isServiceEnabled = prefs.getBoolean("detector_enabled", false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allPermissionsGranted = isOverlayGranted && isAccessibilityEnabled
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Scroll Block",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Break the mindless scrolling loop",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Main Scroll Detector Toggle
        DetectorToggleCard(
            title = "Scroll Detector",
            description = "Monitors rapid/long scrolling",
            isActive = isServiceEnabled,
            isEnabled = allPermissionsGranted,
            onCheckedChange = { enabled ->
                isServiceEnabled = enabled
                prefs.edit().putBoolean("detector_enabled", enabled).apply()
                notifyService(context)
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Setup Requirements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            
            PermissionItem(
                title = "Display Over Apps",
                description = "Required to show the warning overlay",
                isGranted = isOverlayGranted,
                onClick = onOverlayPermissionRequest
            )
            
            PermissionItem(
                title = "Accessibility Service",
                description = "Required to detect scroll patterns",
                isGranted = isAccessibilityEnabled,
                onClick = onAccessibilityPermissionRequest
            )
        }

        // Advanced Options
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Advanced Options",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showAdvanced) {
                            IconButton(onClick = {
                                // Reset to defaults
                                windowMinutes = 5f
                                threshold = 80f
                                idleResetSeconds = 15f
                                debounceMs = 300f
                                overlaySeconds = 3f
                                
                                prefs.edit()
                                    .putLong("pref_window_ms", 300_000L)
                                    .putInt("pref_threshold", 80)
                                    .putLong("pref_idle_reset_ms", 15_000L)
                                    .putLong("pref_debounce_ms", 300L)
                                    .putInt("pref_overlay_time_s", 3)
                                    .apply()
                                notifyService(context)
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults", modifier = Modifier.size(18.dp))
                            }
                        }
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }

                if (showAdvanced) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingSlider(
                        label = "Detection Window",
                        value = windowMinutes,
                        valueRange = 1f..30f,
                        steps = 29,
                        displayValue = "${windowMinutes.toInt()} min",
                        description = "How far back to monitor your scrolling activity.",
                        onValueChange = { 
                            windowMinutes = it
                            prefs.edit().putLong("pref_window_ms", it.toLong() * 60_000L).apply()
                            notifyService(context)
                        }
                    )
                    
                    SettingSlider(
                        label = "Scroll Threshold",
                        value = threshold,
                        valueRange = 10f..300f,
                        steps = 29,
                        displayValue = "${threshold.toInt()} scrolls",
                        description = "Number of scrolls allowed before the block triggers.",
                        onValueChange = { 
                            threshold = it
                            prefs.edit().putInt("pref_threshold", it.toInt()).apply()
                            notifyService(context)
                        }
                    )

                    SettingSlider(
                        label = "Inactivity Reset",
                        value = idleResetSeconds,
                        valueRange = 5f..60f,
                        steps = 11,
                        displayValue = "${idleResetSeconds.toInt()} sec",
                        description = "Time of no scrolling before your progress is cleared.",
                        onValueChange = { 
                            idleResetSeconds = it
                            prefs.edit().putLong("pref_idle_reset_ms", it.toLong() * 1_000L).apply()
                            notifyService(context)
                        }
                    )

                    SettingSlider(
                        label = "Scroll Sensitivity",
                        value = debounceMs,
                        valueRange = 100f..1000f,
                        steps = 18,
                        displayValue = "${debounceMs.toInt()} ms",
                        description = "Minimum gap between movements to count as a unique scroll.",
                        onValueChange = { 
                            debounceMs = it
                            prefs.edit().putLong("pref_debounce_ms", it.toLong()).apply()
                            notifyService(context)
                        }
                    )

                    SettingSlider(
                        label = "Block Duration",
                        value = overlaySeconds,
                        valueRange = 1f..15f,
                        steps = 14,
                        displayValue = "${overlaySeconds.toInt()} sec",
                        description = "Seconds you must wait before you can dismiss the warning.",
                        onValueChange = { 
                            overlaySeconds = it
                            prefs.edit().putInt("pref_overlay_time_s", it.toInt()).apply()
                            notifyService(context)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(visible = !allPermissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Almost there! Please enable both permissions above to start the detector.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Window: ${windowMinutes.toInt()}m | Threshold: ${threshold.toInt()} | Reset: ${idleResetSeconds.toInt()}s",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    description: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun DetectorToggleCard(
    title: String,
    description: String,
    isActive: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled && isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnabled) description else "Permissions required",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = isActive && isEnabled,
                    enabled = isEnabled,
                    onCheckedChange = onCheckedChange
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (!isGranted) 
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) 
        else null,
        onClick = if (isGranted) ({}) else onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isGranted) "✓" else "!",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Text(
                text = if (isGranted) "Enabled" else "Grant",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    for (service in enabledServices) {
        if (service.resolveInfo.serviceInfo.packageName == context.packageName) {
            return true
        }
    }
    return false
}

private fun notifyService(context: Context) {
    val intent = Intent(context, IdleScrollAccessibilityService::class.java)
    context.startService(intent)
}

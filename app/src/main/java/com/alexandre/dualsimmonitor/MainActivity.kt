package com.alexandre.dualsimmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var repository: TelephonyRepository
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = TelephonyRepository(this)
        render()
        val required = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE)
        if (required.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) permissions.launch(required)
    }

    private fun render() {
        setContent {
            MaterialTheme(darkColorScheme(primary = Color(0xFF8BD5CA), secondary = Color(0xFFB7C9C5), background = Color(0xFF101414), surface = Color(0xFF1A2020))) {
                DualSimApp(repository)
            }
        }
    }
}

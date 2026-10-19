package com.mls.painel

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var model: LauncherModel

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) model.gps.start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = LauncherModel(applicationContext)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Launcher não "volta" para lugar nenhum: o botão voltar só fecha painéis abertos.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                model.closeAll()
            }
        })

        setContent {
            LauncherApp(model) { requestLocation() }
        }

        if (!model.gps.hasPermission()) requestLocation()
    }

    private fun requestLocation() {
        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onResume() {
        super.onResume()
        model.tickSystem()
        model.gps.start()
        model.media.restart()
        lifecycleScope.launch { model.reloadApps() }
    }

    override fun onPause() {
        super.onPause()
        model.gps.stop()
        model.media.stop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        model.closeAll()
    }
}

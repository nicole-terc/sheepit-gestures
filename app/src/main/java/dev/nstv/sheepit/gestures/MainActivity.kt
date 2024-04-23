package dev.nstv.sheepit.gestures

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.nstv.sheepit.gestures.ui.screen.SheepScreen
import dev.nstv.sheepit.gestures.ui.screen.bonus.SelectionScreen
import dev.nstv.sheepit.gestures.ui.theme.SheepItGesturesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SheepItGesturesTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SheepScreen()
                }
            }
        }
    }
}

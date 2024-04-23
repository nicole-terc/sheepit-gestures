package dev.nstv.sheepit.gestures.ui.screen.bonus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nstv.composablesheep.library.ComposableSheep
import dev.nstv.composablesheep.library.model.Sheep
import dev.nstv.composablesheep.library.util.SheepColor
import dev.nstv.sheepit.gestures.R
import dev.nstv.sheepit.gestures.ui.screen.SheepScreen

enum class Screen {
    Selector,
    Sheep,
    SheepTower,
    Parallax,
    Step,
}

data class ScreenItem(
    val title: String,
    val sheep: Sheep,
    val brush: Brush? = null,
    val screen: Screen,
)

@Composable
fun SelectionScreen(
    modifier: Modifier = Modifier,
) {
    var selectedScreen by remember { mutableStateOf(Screen.Selector) }

    val nasaBrush = ShaderBrush(ImageShader(ImageBitmap.imageResource(id = R.drawable.nasa_small)))
    val screens = remember {
        listOf(
            ScreenItem(
                title = "Sheep",
                sheep = Sheep(fluffColor = SheepColor.Green),
                screen = Screen.Sheep,
            ),
            ScreenItem(
                title = "Sheep Tower",
                sheep = Sheep(fluffColor = SheepColor.Purple),
                screen = Screen.SheepTower,

                ),
            ScreenItem(
                title = "Parallax Space",
                sheep = Sheep(glassesColor = SheepColor.Magenta, headColor = SheepColor.Black),
                brush = nasaBrush,
                screen = Screen.Parallax,
            ),
            ScreenItem(
                title = "Step Screen",
                sheep = Sheep(fluffColor = SheepColor.Orange),
                screen = Screen.Step,
            )
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {

        BackHandler(selectedScreen != Screen.Selector) {
            selectedScreen = Screen.Selector
        }

        Crossfade(
            targetState = selectedScreen, label = "Screen Crossfade",
        ) { screen ->
            when (screen) {
                Screen.Selector -> ViewSelectionScreen(
                    modifier = modifier,
                    screens = screens,
                    onItemClick = { selectedScreen = it },
                )

                Screen.Sheep -> SheepScreen()
                Screen.SheepTower -> SheepTowerScreen()
                Screen.Parallax -> ParallaxScreen()
                Screen.Step -> StepCounterScreen()
            }
        }
    }
}


@Composable
fun ViewSelectionScreen(
    modifier: Modifier = Modifier,
    screens: List<ScreenItem>,
    onItemClick: (Screen) -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier.padding(8.dp),
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(screens) { screenItem ->
            ScreenItemCard(
                screenItem = screenItem,
                onClick = { onItemClick(screenItem.screen) },
            )
        }
    }

}

@Composable
fun ScreenItemCard(
    screenItem: ScreenItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.clickable { onClick() }) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (screenItem.brush != null) {
                ComposableSheep(
                    sheep = screenItem.sheep,
                    fluffBrush = screenItem.brush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            } else {
                ComposableSheep(
                    sheep = screenItem.sheep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }
            Text(
                text = screenItem.title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
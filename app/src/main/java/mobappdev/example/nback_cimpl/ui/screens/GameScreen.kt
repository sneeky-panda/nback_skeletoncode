package mobappdev.example.nback_cimpl.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun GameScreen(vm: GameViewModel, nav: () -> Unit) {

    // Observera spelstatus och poäng från ViewModel
    val gameState by vm.gameState.collectAsState()
    val currentScore by vm.score.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "N-back Game",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(16.dp)

            )

            // Visa aktuell poäng
            Text(
                text = "Current Score: $currentScore",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "${vm.gameState.value.toString()} Där ser man "

            )

            //3x3
            Column( verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ){
                for(i in 0 until 3){
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        for(j in 0 until 3){
                            val position = i *3+ j+1
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(2.dp, Color.Black)
                                    .background(
                                        if(gameState.eventValue ==position) Color.Green else Color.Transparent
                                    ).clickable {
                                        scope.launch {
                                            val isCorret = vm.processUserAction(position)
                                            snackBarHostState.showSnackbar(
                                                if(isCorret) "Correct!" else "incorrect!"
                                            )
                                        }
                                    }, contentAlignment = Alignment.Center
                            ){ Text(
                                text = position.toString(),
                                style = MaterialTheme.typography.bodyLarge,

                            )
                            }


                        }
                    }
                }
            }


            // Knapp för att navigera tillbaka till HomeScreen
            Button(
                onClick = {nav()
                    vm.updateHighScoreIfNeeded()
                          },
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(text = "Back to Home")
            }
            if (gameState.eventValue != -1) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Current eventValue is: ${gameState.eventValue}",
                    textAlign = TextAlign.Center
                )
            }
            Button(onClick = vm::startGame) {

                Text(text = "Generate eventValues")
            }
            Button(onClick = {
                vm.checkMatch() // Kontrollera om poäng ska ges
            }) {
                Text("Check Match")
            }



            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {


            }
        }
    }
}
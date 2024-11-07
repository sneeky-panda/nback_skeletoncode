package mobappdev.example.nback_cimpl.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun updateHighScoreIfNeeded()
    fun setGameType(gameType: GameType)
    fun startGame()
    fun resetCurrentValue()
    fun checkMatch()
    fun processUserAction(position: Int): Boolean
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel() {

    private var currentEventIndex = -1
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    override fun processUserAction(position: Int): Boolean {
        val isCorrect = position == _gameState.value.eventValue
        if (isCorrect) {
            _score.value += 1
        }
        return isCorrect
    }


    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    // nBack is currently hardcoded
    override val nBack: Int = 2

    override fun updateHighScoreIfNeeded() {
        //if (_score.value > _highscore.value) {
        _highscore.value = 0 // Uppdatera högsta poängen i ViewModel

        // Starta en coroutine för att spara högsta poängen permanent
        viewModelScope.launch {
            userPreferencesRepository.saveHighScore(_highscore.value)

        }
    }

    private var job: Job? = null  // coroutine job for the game event
    private val eventInterval: Long = 2000L  // 2000 ms (2s)

    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events = emptyArray<Int>()  // Array with all events

    override fun setGameType(gameType: GameType) {
        // update the gametype in the gamestate
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {

        job?.cancel()  // Cancel any existing game loop
        currentEventIndex = -1
        //guess= Guess.NONE
        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)

        events = nBackHelper.generateNBackString(10, 9, 30, nBack).toList()
                .toTypedArray()
       // Todo Higher Grade: currently the size etc. are hardcoded, make these based on user input
        Log.d("GameVM", "The following sequence was generated: ${events.contentToString()}")

        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame(events)
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }
            // Todo: update the highscore
        }
    }

    override fun resetCurrentValue() {
        _score.value = 0
    }


    override fun checkMatch() {

        val c = events[currentEventIndex]
        Log.d("GameVM", "${c.toString()}")
        // Kontrollera om det finns tillräckligt många tidigare event för att jämföra
        if (currentEventIndex  >= nBack) {
            val currentEventValue = events[currentEventIndex]
            val nBackEvent = events[currentEventIndex - nBack]
            Log.d(
                "GameVM",
                "current  ${currentEventValue.toString()} och nBack ${nBackEvent.toString()}"
            )
            // Kontrollera om det aktuella värdet matchar n-back-värdet
            if (currentEventValue == nBackEvent) {
                // Om matchning, öka poängen
                _score.value += 1
                // Uppdatera och spara högsta poängen om det behövs
                if (_score.value > _highscore.value) {
                    _highscore.value = _score.value
                    viewModelScope.launch {
                        userPreferencesRepository.saveHighScore(_highscore.value)
                        Log.d("GameVM", "New high score saved: ${_highscore.value}")
                    }
                }
            } else {
                Log.d("GameVM", "Detta är test: ${currentEventValue} och ${nBackEvent}")
                Log.d("GameVM", "No match")

            }
        } else {
            Log.d("GameVM", "Not enough events to check n-back match.")
            Log.d("GameVM", "Kollar av  ${c.toString()}")
        }
        /**
         * Todo: This function should check if there is a match when the user presses a match button
         * Make sure the user can only register a match once for each event.
         */
    }

    private fun runAudioGame() {
        // Todo: Make work for Basic grade

    }

    private suspend fun runVisualGame(events: Array<Int>) {
        // Todo: Replace this code for actual game code
        for (value in events) {
            currentEventIndex++
            _gameState.value = _gameState.value.copy(eventValue = value)
            delay(eventInterval)
        }

    }

    private fun runAudioVisualGame() {
        // Todo: Make work for Higher grade
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository)
            }
        }
    }

    init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }

}

// Class with the different game types
enum class GameType {
    Audio,
    Visual,
    AudioVisual
}

data class GameState(
    // You can use this state to push values from the VM to your UI.
    val gameType: GameType = GameType.Visual,  // Type of the game
    val eventValue: Int = -1  // The value of the array string
)

class FakeVM : GameViewModel {
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val nBack: Int
        get() = 2

    override fun updateHighScoreIfNeeded() {
        TODO("Not yet implemented")
    }

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun resetCurrentValue() {
        TODO("Not yet implemented")
    }

    override fun checkMatch() {
    }

    override fun processUserAction(position: Int): Boolean {
        TODO("Not yet implemented")
    }
}
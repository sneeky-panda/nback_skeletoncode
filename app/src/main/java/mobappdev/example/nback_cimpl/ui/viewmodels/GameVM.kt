package mobappdev.example.nback_cimpl.ui.viewmodels

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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
import java.util.Locale

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

    fun intToLetter(value: Int): String
    fun updateHighScoreIfNeeded()
    fun setGameType(gameType: GameType)
    fun startGame()
    fun resetCurrentValue()
    fun checkMatch()
    fun processUserAction(position: Int): Boolean
    fun onInit(status: Int)
    fun speak(text: String)
    fun onCleared()
    fun initializeTextToSpeech()
}

class GameVM(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, AndroidViewModel(application), TextToSpeech.OnInitListener {

    var currentEventNumber: Int = 0 // Eventets ordningsnummer i rundan
    var correctResponses: Int = 0
    private lateinit var textToSpeech: TextToSpeech
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
               // GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }
            // Todo: update the highscore
        }
    }

    override fun resetCurrentValue() {
        _score.value = 0
    }


    override fun checkMatch() {

       // val c = events[currentEventIndex]
       // Log.d("GameVM", "${c.toString()}")
        // Kontrollera om det finns tillräckligt många tidigare event för att jämföra
        if (currentEventIndex  >= nBack) {

            val currentEventValue = events[currentEventIndex]
            val nBackEvent = events[currentEventIndex - nBack]
            // Kontrollera om det aktuella värdet matchar n-back-värdet
            if (currentEventValue == nBackEvent && _gameState.value.isGuessed==false) {
                // Om matchning, öka poängen
                _gameState.value.isGuessed = true
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
                Log.d("GameVM", "No match")

            }
        } else {
            Log.d("GameVM", "Not enough events to check n-back match.")

        }
        /**
         * Todo: This function should check if there is a match when the user presses a match button
         * Make sure the user can only register a match once for each event.
         */
    }
     private fun playAudioStimulus(value: Int) {
         val letter = intToLetter(value)
         Log.d("GameVM", "Playing audio for event: $letter")
         speak(letter)  // Använd TTS för att läsa upp bokstaven
        // Använd `letter` för att spela upp rätt ljudfil, t.ex. "A.mp3" för "A"

     }


    override fun intToLetter(value:Int): String{
        return when(value){
            1 -> "A"
            2 -> "B"
            3 -> "C"
            4 -> "D"
            5 -> "E"
            6 -> "F"
            7 -> "G"
            8 -> "H"
            9 -> "I"
            else -> "?"  // Använd "?" om inget matchande värde hittas
        }
    }
    private suspend fun runAudioGame(events: Array<Int>) {
        for (value in events) {
            currentEventIndex++
            _gameState.value = _gameState.value.copy(eventValue = value,currentEventNumber = currentEventIndex + 1 )
            playAudioStimulus(value)  // Spela upp ljud för varje event
            delay(eventInterval)  // Vänta en stund innan nästa event spelas upp
            // Öka eventnumret
        }
        // Todo: Make work for Basic grade
    }

    private suspend fun runVisualGame(events: Array<Int>) {
        // Todo: Replace this code for actual game code
        for (value in events) {
            currentEventIndex++
            _gameState.value = _gameState.value.copy(eventValue = value,currentEventNumber = currentEventIndex + 1 )
            delay(eventInterval)
        }

    }

    private fun runAudioVisualGame() {
        // Todo: Make work for Higher grade
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                val userPreferencesRepository = (application as GameApplication).userPreferencesRespository
                GameVM(application, userPreferencesRepository)
            }
        }
    }
    init {
        // Code that runs during creation of the vm
        textToSpeech = TextToSpeech(getApplication(), this)

        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown() // Frigör TTS-resurser
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // The TTS engine was initialized successfully
            val langResult = textToSpeech?.setLanguage(Locale.ENGLISH)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("GameVM", "Language is not supported or missing data")
            } else {
                Log.d("GameVM", "Text-to-Speech Initialized successfully")
            }
        } else {
            Log.e("GameVM", "Text-to-Speech initialization failed")
        }
    }
    override fun initializeTextToSpeech() {
        if (!::textToSpeech.isInitialized || textToSpeech == null) {
            textToSpeech = TextToSpeech(getApplication(), this)
        }
    }

    override fun speak(text: String) {
        textToSpeech?.let {
            if (it.isSpeaking) {
                it.stop() // Stop speaking if something is already being spoken
            }
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) // Speak the event
        }
    }

  /*  init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
*/
}

// Class with the different game types
enum class GameType {
    Audio,
    Visual/*,
    AudioVisual*/
}

data class GameState(
    // You can use this state to push values from the VM to your UI.
    val gameType: GameType = GameType.Visual,  // Type of the game
    val eventValue: Int = -1,  // The value of the array string
    val currentEventNumber: Int = 0,         // Aktuellt eventnummer
    val correctResponses: Int = 0,
    var isGuessed: Boolean = false
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

    override fun intToLetter(value: Int): String {
        TODO("Not yet implemented")
    }

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

    override fun onInit(status: Int) {
        TODO("Not yet implemented")
    }

    override fun speak(text: String) {
        TODO("Not yet implemented")
    }

    override fun onCleared() {
        TODO("Not yet implemented")
    }

    override fun initializeTextToSpeech() {
        TODO("Not yet implemented")
    }
}
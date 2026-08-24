package com.example.network

import com.example.data.models.GameState
import com.example.data.models.GameStatus
import com.example.data.models.Player
import org.json.JSONArray
import org.json.JSONObject

/**
 * Protocolo de serialización/deserialización JSON ligero para mensajes
 * transmitidos a través de Nearby Connections.
 */
object MessageProtocol {

    private const val TYPE = "type"
    private const val TYPE_GAME_STATE = "GAME_STATE"
    private const val TYPE_GUESS_LETTER = "GUESS_LETTER"
    private const val TYPE_START_GAME = "START_GAME"
    private const val TYPE_PLAYER_JOIN = "PLAYER_JOIN"
    private const val TYPE_RESET_GAME = "RESET_GAME"
    private const val TYPE_REVEAL_HINT = "REVEAL_HINT"

    // Payloads
    private const val KEY_LETTER = "letter"
    private const val KEY_PLAYER_ID = "player_id"
    private const val KEY_PLAYER_NAME = "player_name"
    private const val KEY_SECRET_WORD = "secret_word"
    private const val KEY_CATEGORY = "category"
    private const val KEY_HINT = "hint"
    private const val KEY_HINT_REVEALED = "hint_revealed"
    private const val KEY_REVEALED = "revealed"
    private const val KEY_GUESSED = "guessed"
    private const val KEY_ERRORS = "errors"
    private const val KEY_MAX_ERRORS = "max_errors"
    private const val KEY_STATUS = "status"
    private const val KEY_PLAYERS = "players"
    private const val KEY_WORD_CREATOR = "word_creator"
    private const val KEY_CURRENT_TURN = "current_turn"
    private const val KEY_LAST_LETTER = "last_letter"
    private const val KEY_LAST_PLAYER = "last_player"
    private const val KEY_LAST_CORRECT = "last_correct"
    private const val KEY_TIMER_SEC = "timer_sec"

    fun serializeGameState(state: GameState): ByteArray {
        val json = JSONObject().apply {
            put(TYPE, TYPE_GAME_STATE)
            put(KEY_SECRET_WORD, state.secretWord)
            put(KEY_CATEGORY, state.category)
            put(KEY_HINT, state.hint)
            put(KEY_HINT_REVEALED, state.hintRevealed)
            put(KEY_REVEALED, JSONArray(state.revealedLetters.map { it.toString() }))
            put(KEY_GUESSED, JSONArray(state.guessedLetters.map { it.toString() }))
            put(KEY_ERRORS, state.errors)
            put(KEY_MAX_ERRORS, state.maxErrors)
            put(KEY_STATUS, state.status.name)
            put(KEY_WORD_CREATOR, state.wordCreatorPlayerId)
            put(KEY_CURRENT_TURN, state.currentTurnPlayerId)
            put(KEY_TIMER_SEC, state.turnTimeRemainingSec)
            state.lastGuessedLetter?.let { put(KEY_LAST_LETTER, it.toString()) }
            state.lastGuessedPlayerId?.let { put(KEY_LAST_PLAYER, it) }
            state.lastGuessedCorrect?.let { put(KEY_LAST_CORRECT, it) }

            val playersArray = JSONArray()
            state.players.forEach { p ->
                val pJson = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("isHost", p.isHost)
                    put("colorIndex", p.colorIndex)
                    put("score", p.score)
                }
                playersArray.put(pJson)
            }
            put(KEY_PLAYERS, playersArray)
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun serializeGuessLetter(letter: Char, playerId: String): ByteArray {
        val json = JSONObject().apply {
            put(TYPE, TYPE_GUESS_LETTER)
            put(KEY_LETTER, letter.toString())
            put(KEY_PLAYER_ID, playerId)
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun serializeStartGame(secretWord: String, category: String = "General", hint: String = ""): ByteArray {
        val json = JSONObject().apply {
            put(TYPE, TYPE_START_GAME)
            put(KEY_SECRET_WORD, secretWord)
            put(KEY_CATEGORY, category)
            put(KEY_HINT, hint)
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun serializePlayerJoin(name: String, playerId: String): ByteArray {
        val json = JSONObject().apply {
            put(TYPE, TYPE_PLAYER_JOIN)
            put(KEY_PLAYER_NAME, name)
            put(KEY_PLAYER_ID, playerId)
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun serializeResetGame(): ByteArray {
        val json = JSONObject().apply {
            put(TYPE, TYPE_RESET_GAME)
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun serializeRevealHint(): ByteArray {
        val json = JSONObject().apply {
            put(TYPE, TYPE_REVEAL_HINT)
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    sealed class ParsedMessage {
        data class GameStateMessage(val state: GameState) : ParsedMessage()
        data class GuessLetterMessage(val letter: Char, val playerId: String) : ParsedMessage()
        data class StartGameMessage(val secretWord: String, val category: String = "General", val hint: String = "") : ParsedMessage()
        data class PlayerJoinMessage(val name: String, val playerId: String) : ParsedMessage()
        object ResetGameMessage : ParsedMessage()
        object RevealHintMessage : ParsedMessage()
        object Unknown : ParsedMessage()
    }

    fun parseMessage(bytes: ByteArray): ParsedMessage {
        return try {
            val str = String(bytes, Charsets.UTF_8)
            val json = JSONObject(str)
            when (json.optString(TYPE)) {
                TYPE_GAME_STATE -> {
                    val revealedArray = json.optJSONArray(KEY_REVEALED) ?: JSONArray()
                    val revealed = mutableSetOf<Char>()
                    for (i in 0 until revealedArray.length()) {
                        revealed.add(revealedArray.getString(i)[0])
                    }

                    val guessedArray = json.optJSONArray(KEY_GUESSED) ?: JSONArray()
                    val guessed = mutableSetOf<Char>()
                    for (i in 0 until guessedArray.length()) {
                        guessed.add(guessedArray.getString(i)[0])
                    }

                    val playersArray = json.optJSONArray(KEY_PLAYERS) ?: JSONArray()
                    val players = mutableListOf<Player>()
                    for (i in 0 until playersArray.length()) {
                        val pObj = playersArray.getJSONObject(i)
                        players.add(
                            Player(
                                id = pObj.getString("id"),
                                name = pObj.getString("name"),
                                isHost = pObj.optBoolean("isHost", false),
                                colorIndex = pObj.optInt("colorIndex", 0),
                                score = pObj.optInt("score", 0)
                            )
                        )
                    }

                    val lastLetterStr = json.optString(KEY_LAST_LETTER, "")
                    val lastLetter = if (lastLetterStr.isNotEmpty()) lastLetterStr[0] else null

                    val state = GameState(
                        secretWord = json.optString(KEY_SECRET_WORD, ""),
                        category = json.optString(KEY_CATEGORY, "General"),
                        hint = json.optString(KEY_HINT, ""),
                        hintRevealed = json.optBoolean(KEY_HINT_REVEALED, false),
                        revealedLetters = revealed,
                        guessedLetters = guessed,
                        errors = json.optInt(KEY_ERRORS, 0),
                        maxErrors = json.optInt(KEY_MAX_ERRORS, 6),
                        status = GameStatus.valueOf(json.optString(KEY_STATUS, GameStatus.WAITING_PLAYERS.name)),
                        players = players,
                        wordCreatorPlayerId = json.optString(KEY_WORD_CREATOR, ""),
                        currentTurnPlayerId = json.optString(KEY_CURRENT_TURN, ""),
                        lastGuessedLetter = lastLetter,
                        lastGuessedPlayerId = if (json.has(KEY_LAST_PLAYER)) json.getString(KEY_LAST_PLAYER) else null,
                        lastGuessedCorrect = if (json.has(KEY_LAST_CORRECT)) json.getBoolean(KEY_LAST_CORRECT) else null,
                        turnTimeRemainingSec = json.optInt(KEY_TIMER_SEC, 20)
                    )
                    ParsedMessage.GameStateMessage(state)
                }
                TYPE_GUESS_LETTER -> {
                    val letter = json.getString(KEY_LETTER)[0]
                    val playerId = json.getString(KEY_PLAYER_ID)
                    ParsedMessage.GuessLetterMessage(letter, playerId)
                }
                TYPE_START_GAME -> {
                    val word = json.getString(KEY_SECRET_WORD)
                    val cat = json.optString(KEY_CATEGORY, "General")
                    val hint = json.optString(KEY_HINT, "")
                    ParsedMessage.StartGameMessage(word, cat, hint)
                }
                TYPE_PLAYER_JOIN -> {
                    val name = json.getString(KEY_PLAYER_NAME)
                    val id = json.getString(KEY_PLAYER_ID)
                    ParsedMessage.PlayerJoinMessage(name, id)
                }
                TYPE_RESET_GAME -> ParsedMessage.ResetGameMessage
                TYPE_REVEAL_HINT -> ParsedMessage.RevealHintMessage
                else -> ParsedMessage.Unknown
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ParsedMessage.Unknown
        }
    }
}

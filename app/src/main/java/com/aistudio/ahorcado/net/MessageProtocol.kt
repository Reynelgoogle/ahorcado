package com.aistudio.ahorcado.net

import com.aistudio.ahorcado.domain.model.GamePhase
import com.aistudio.ahorcado.domain.model.Player
import com.aistudio.ahorcado.domain.model.PublicState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Protocolo JSON sobre Nearby Connections.
 *
 * REGLA DE SEGURIDAD: la palabra secreta nunca viaja en [PublicState.secretWord]
 * durante la ronda; solo se incluye cuando la fase es ROUND_OVER (revelado final).
 */
object MessageProtocol {

    const val T_HELLO = "HELLO"         // cliente -> host: {pid, name}
    const val T_STATE = "STATE"         // host -> todos: estado público
    const val T_GUESS = "GUESS"         // cliente -> host: {pid, letter}
    const val T_HINT = "HINT"           // cliente -> host: {pid} pide revelar pista
    const val T_KICKED = "KICKED"       // host -> cliente: expulsado
    const val T_ROOM_FULL = "ROOM_FULL" // host -> cliente: sala llena o en curso
    const val T_LEAVE = "LEAVE"         // cliente -> host: {pid} se va

    sealed interface NetMessage {
        data class Hello(val playerId: String, val name: String) : NetMessage
        data class State(val state: PublicState) : NetMessage
        data class Guess(val playerId: String, val letter: Char) : NetMessage
        data class Hint(val playerId: String) : NetMessage
        data object Kicked : NetMessage
        data object RoomFull : NetMessage
        data class Leave(val playerId: String) : NetMessage
        data object Unknown : NetMessage
    }

    // ---------- serialización ----------

    fun hello(playerId: String, name: String): ByteArray =
        JSONObject()
            .put("t", T_HELLO)
            .put("pid", playerId)
            .put("name", name)
            .toString().toByteArray(Charsets.UTF_8)

    fun guess(playerId: String, letter: Char): ByteArray =
        JSONObject()
            .put("t", T_GUESS)
            .put("pid", playerId)
            .put("letter", letter.toString())
            .toString().toByteArray(Charsets.UTF_8)

    fun hint(playerId: String): ByteArray =
        JSONObject()
            .put("t", T_HINT)
            .put("pid", playerId)
            .toString().toByteArray(Charsets.UTF_8)

    fun kicked(): ByteArray =
        JSONObject().put("t", T_KICKED).toString().toByteArray(Charsets.UTF_8)

    fun roomFull(): ByteArray =
        JSONObject().put("t", T_ROOM_FULL).toString().toByteArray(Charsets.UTF_8)

    fun leave(playerId: String): ByteArray =
        JSONObject()
            .put("t", T_LEAVE)
            .put("pid", playerId)
            .toString().toByteArray(Charsets.UTF_8)

    fun state(s: PublicState): ByteArray {
        val players = JSONArray()
        s.players.forEach { p ->
            players.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("host", p.isHost)
                    .put("color", p.colorIndex)
                    .put("score", p.score)
                    .put("conn", p.connected),
            )
        }
        val json = JSONObject()
            .put("t", T_STATE)
            .put("phase", s.phase.name)
            .put("wordLen", s.wordLength)
            .put("category", s.category)
            .put("hint", s.hint)
            .put("hintRev", s.hintRevealed)
            .put("revealed", s.revealed.joinToString(""))
            .put("guessed", s.guessed.joinToString(""))
            .put("errors", s.errors)
            .put("maxErrors", s.maxErrors)
            .put("players", players)
            .put("creatorId", s.creatorId)
            .put("turnId", s.turnPlayerId)
            .put("deadline", s.turnDeadline)
            .put("turnSec", s.turnSeconds)
            .put("winnerId", s.winnerId ?: "")
            .put("pattern", s.pattern)
        s.lastLetter?.let { json.put("lastLetter", it.toString()) }
        s.lastPlayerId?.let { json.put("lastPid", it) }
        s.lastCorrect?.let { json.put("lastOk", it) }
        // La palabra solo viaja al terminar la ronda.
        if (s.phase == GamePhase.ROUND_OVER) {
            json.put("secret", s.secretWord)
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    // ---------- deserialización ----------

    fun parse(bytes: ByteArray): NetMessage {
        return try {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            when (json.optString("t")) {
                T_HELLO -> NetMessage.Hello(
                    playerId = json.getString("pid"),
                    name = json.getString("name"),
                )
                T_STATE -> NetMessage.State(parseState(json))
                T_GUESS -> {
                    val letter = json.optString("letter", "")
                    if (letter.length != 1) NetMessage.Unknown
                    else NetMessage.Guess(json.getString("pid"), letter[0])
                }
                T_HINT -> NetMessage.Hint(json.getString("pid"))
                T_KICKED -> NetMessage.Kicked
                T_ROOM_FULL -> NetMessage.RoomFull
                T_LEAVE -> NetMessage.Leave(json.getString("pid"))
                else -> NetMessage.Unknown
            }
        } catch (e: Exception) {
            NetMessage.Unknown
        }
    }

    private fun parseState(json: JSONObject): PublicState {
        val players = mutableListOf<Player>()
        val arr = json.optJSONArray("players") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            players += Player(
                id = o.optString("id"),
                name = o.optString("name"),
                isHost = o.optBoolean("host", false),
                colorIndex = o.optInt("color", 0),
                score = o.optInt("score", 0),
                connected = o.optBoolean("conn", true),
            )
        }
        val phase = try {
            GamePhase.valueOf(json.optString("phase", GamePhase.LOBBY.name))
        } catch (e: IllegalArgumentException) {
            GamePhase.LOBBY
        }
        val lastLetterStr = json.optString("lastLetter", "")
        return PublicState(
            phase = phase,
            wordLength = json.optInt("wordLen", 0),
            category = json.optString("category", ""),
            hint = json.optString("hint", ""),
            hintRevealed = json.optBoolean("hintRev", false),
            revealed = json.optString("revealed", "").toSet(),
            guessed = json.optString("guessed", "").toSet(),
            errors = json.optInt("errors", 0),
            maxErrors = json.optInt("maxErrors", 6),
            players = players,
            creatorId = json.optString("creatorId", ""),
            turnPlayerId = json.optString("turnId", ""),
            turnDeadline = json.optLong("deadline", 0L),
            turnSeconds = json.optInt("turnSec", 20),
            lastLetter = lastLetterStr.firstOrNull(),
            lastPlayerId = json.optString("lastPid", "").ifEmpty { null },
            lastCorrect = if (json.has("lastOk")) json.optBoolean("lastOk") else null,
            winnerId = json.optString("winnerId", "").ifEmpty { null },
            pattern = json.optString("pattern", ""),
            secretWord = json.optString("secret", ""),
        )
    }
}

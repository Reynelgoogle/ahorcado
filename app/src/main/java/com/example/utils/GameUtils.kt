package com.example.utils

import androidx.compose.ui.graphics.Color
import com.example.data.models.CategorizedWord
import com.example.data.models.WordCategory

object GameUtils {

    val Categories: List<WordCategory> = listOf(
        WordCategory(
            id = "movies",
            name = "Cine y Series",
            icon = "🎬",
            description = "Películas taquilleras, clásicos y personajes",
            words = listOf(
                CategorizedWord("INTERSTELLAR", "Viajes en el tiempo, agujeros negros y amor cuántico"),
                CategorizedWord("GLADIADOR", "Máximo Décimo Meridio en el Coliseo romano"),
                CategorizedWord("TITANIC", "Un iceberg y un barco que ni Dios podía hundir"),
                CategorizedWord("INCEPTION", "Robo de secretos a través de los sueños"),
                CategorizedWord("BATMAN", "El caballero de la noche de Ciudad Gótica"),
                CategorizedWord("MATRIX", "Pastilla roja o pastilla azul en una simulación"),
                CategorizedWord("AVATAR", "Seres azules y el planeta Pandora")
            )
        ),
        WordCategory(
            id = "science",
            name = "Ciencia y Espacio",
            icon = "🚀",
            description = "Astronomía, física y maravillas del universo",
            words = listOf(
                CategorizedWord("ASTRONAUTA", "Persona que viaja más allá de la atmósfera terrestre"),
                CategorizedWord("SUPERNOVA", "Explosión colosal al final de la vida de una estrella"),
                CategorizedWord("GRAVEDAD", "Fuerza invisible que mantiene los planetas en órbita"),
                CategorizedWord("TELESCOPIO", "Instrumento para observar galaxias lejanas"),
                CategorizedWord("RELATIVIDAD", "Teoría revolucionaria de Albert Einstein"),
                CategorizedWord("ATMOSFERA", "Capa de gases que protege y da vida a nuestro planeta")
            )
        ),
        WordCategory(
            id = "gaming",
            name = "Videojuegos",
            icon = "🎮",
            description = "Iconos del gaming, consolas y universos",
            words = listOf(
                CategorizedWord("MINECRAFT", "Mundo infinito de bloques y crafteo"),
                CategorizedWord("POKEMON", "Criaturas de bolsillo que entrenas para luchar"),
                CategorizedWord("ZELDA", "La leyenda del héroe de Hyrule Link"),
                CategorizedWord("FORTNITE", "Battle Royale con construcciones y bailes"),
                CategorizedWord("PLAYSTATION", "Mítica consola japonesa de Sony"),
                CategorizedWord("CYBERPUNK", "Futuro distópico con implantes y luces de neón")
            )
        ),
        WordCategory(
            id = "geography",
            name = "Países y Lugares",
            icon = "🌍",
            description = "Geografía mundial y destinos asombrosos",
            words = listOf(
                CategorizedWord("ARGENTINA", "Tierra del tango, el asado y la Patagonia"),
                CategorizedWord("JAPON", "País del sol naciente, anime y cerezos"),
                CategorizedWord("AUSTRALIA", "Hogar de canguros, koalas y la Gran Barrera"),
                CategorizedWord("MADAGASCAR", "Gran isla africana con flora y fauna única"),
                CategorizedWord("ISLANDIA", "Tierra de fuego, hielo, cascadas y auroras"),
                CategorizedWord("COLOMBIA", "Famoso por su café, vallenato y biodiversidad")
            )
        ),
        WordCategory(
            id = "food",
            name = "Gastronomía",
            icon = "🍕",
            description = "Platillos deliciosos y delicias culinarias",
            words = listOf(
                CategorizedWord("CHOCOLATE", "Delicia dulce hecha con semillas de cacao"),
                CategorizedWord("HAMBURGUESA", "Carne entre dos panes con queso y salsas"),
                CategorizedWord("ESPAGUETI", "Pasta larga italiana servida con salsa boloñesa"),
                CategorizedWord("GUACAMOLE", "Salsa mexicana tradicional hecha con aguacate fresco"),
                CategorizedWord("CROISSANT", "Pan hojaldrado con forma de media luna")
            )
        )
    )

    // Palabras de ejemplo planas para modo rápido
    val SampleWords = Categories.flatMap { it.words.map { cw -> cw.word } }

    // Paleta de colores para los jugadores (1 a 4)
    val PlayerColors = listOf(
        Color(0xFF6366F1), // Índigo / Violeta
        Color(0xFF10B981), // Esmeralda
        Color(0xFFF59E0B), // Ámbar / Naranja
        Color(0xFFEC4899)  // Rosa fucsia
    )

    fun getPlayerColor(colorIndex: Int): Color {
        return PlayerColors.getOrElse(colorIndex % PlayerColors.size) { Color(0xFF6366F1) }
    }
}

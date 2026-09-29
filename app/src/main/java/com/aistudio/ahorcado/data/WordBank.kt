package com.aistudio.ahorcado.data

/**
 * Banco de palabras para la opción "al azar". La validación normaliza
 * tildes automáticamente, así que las palabras pueden llevarlas.
 */
object WordBank {

    data class Entry(val word: String, val hint: String)
    data class Category(val name: String, val icon: String, val entries: List<Entry>)

    val categories: List<Category> = listOf(
        Category(
            "Animales", "🐾", listOf(
                Entry("JIRAFA", "Cuello muy largo"),
                Entry("ELEFANTE", "El más grande de la sabana"),
                Entry("COCODRILO", "Reptil de mandíbula temible"),
                Entry("MURCIELAGO", "Mamífero que vuela de noche"),
                Entry("CANGURO", "Salta y lleva a su cría en el bolso"),
                Entry("PINGUINO", "Ave que no vuela y vive en el hielo"),
                Entry("TORTUGA", "Lleva su casa a cuestas"),
                Entry("DELFIN", "Mamífero marino muy inteligente"),
                Entry("ARDILLA", "Guarda nueces para el invierno"),
                Entry("CAMALEON", "Cambia de color"),
                Entry("HIPOPOTAMO", "Gigante que vive en ríos de África"),
                Entry("MARIPOSA", "Oruga transformada con alas de colores"),
            ),
        ),
        Category(
            "Comida", "🍍", listOf(
                Entry("MANGO", "Fruta tropical amarilla"),
                Entry("CROQUETA", "Frita y típica en Cuba"),
                Entry("TAMAL", "Maíz envuelto en hoja"),
                Entry("PAELLA", "Arroz español con mariscos"),
                Entry("CHOCOLATE", "Dulce del cacao"),
                Entry("SANDWICH", "Pan con relleno en medio"),
                Entry("PIZZA", "Italiana, redonda y con queso"),
                Entry("HELADO", "Frío y dulce"),
                Entry("YUCA", "Vianda que se hierve"),
                Entry("PLATANO", "Se fríe en tostones"),
                Entry("ARROZ", "Base de casi toda comida cubana"),
                Entry("EMPANADA", "Masa rellena doblada"),
            ),
        ),
        Category(
            "Lugares", "🗺️", listOf(
                Entry("PLAYA", "Arena y mar"),
                Entry("MONTAÑA", "Muy alta, con pico"),
                Entry("BIBLIOTECA", "Llena de libros"),
                Entry("HOSPITAL", "Atienden enfermos"),
                Entry("MERCADO", "Se compra comida"),
                Entry("PARQUE", "Árboles y bancos en la ciudad"),
                Entry("CINE", "Pantalla gigante"),
                Entry("ESCUELA", "Niños y maestros"),
                Entry("AEROPUERTO", "Despegan aviones"),
                Entry("CASTILLO", "Fortaleza de reyes"),
                Entry("DESIERTO", "Arena y mucho calor"),
                Entry("ISLA", "Tierra rodeada de agua"),
            ),
        ),
        Category(
            "Objetos", "💡", listOf(
                Entry("TELEFONO", "Sirve para llamar"),
                Entry("LAMPARA", "Da luz"),
                Entry("ESPEJO", "Te ves en él"),
                Entry("MOCHILA", "Se lleva en la espalda"),
                Entry("RELOJ", "Mide el tiempo"),
                Entry("TIJERAS", "Cortan papel"),
                Entry("PARAGUAS", "Protege de la lluvia"),
                Entry("GUITARRA", "Instrumento de cuerdas"),
                Entry("BRUJULA", "Señala el norte"),
                Entry("MALETA", "Para viajar"),
                Entry("VELA", "Luz con cera"),
                Entry("LLAVE", "Abre puertas"),
            ),
        ),
    )

    data class Pick(val word: String, val category: String, val hint: String)

    fun random(): Pick {
        val cat = categories.random()
        val e = cat.entries.random()
        return Pick(e.word, cat.name, e.hint)
    }
}

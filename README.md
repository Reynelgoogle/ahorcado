# Ahorcado P2P v2 — rebuild desde cero

Juego del ahorcado **multijugador local** (hasta 4 jugadores) por
Nearby Connections (Bluetooth + Wi-Fi, sin internet), más modo
**juego local** (2+ jugadores en un mismo teléfono).

Reescritura completa de la v1: arquitectura por capas, host autoritativo,
protocolo endurecido y estética doodle tomada del icono del juego.

## Requisitos

- Android Studio (Ladybug o posterior) con JDK 17
- `minSdk 24`, `targetSdk 35`
- Para probar el multijugador: 2 teléfonos con Bluetooth y Wi-Fi encendidos
  (no hace falta internet)

## Compilar e instalar

```bash
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Tests unitarios del dominio (JUnit, corren en la JVM):

```bash
./gradlew :app:testDebugUnitTest
```

## Probar el multijugador

1. Teléfono A: **Crear sala** → queda anunciándose.
2. Teléfono B: **Unirse a sala** → toca la sala de A.
3. A: **Elegir palabra** (o 🎲 al azar) → **¡A jugar!**
4. Juegan por turnos con 20 s cada uno. Puntos: 10 por letra acertada
   (× apariciones) + 50 de bono por completar la palabra.

## Decisiones de diseño (corrigen bugs de la v1)

- **La palabra secreta nunca viaja en claro**: los clientes solo reciben
  longitud, patrón enmascarado (`C_S_`) y letras reveladas. La palabra
  completa solo se envía en `ROUND_OVER` para el revelado final.
- **El host valida todo**: cada `GUESS` se verifica (fase, turno actual,
  identidad extremo→jugador, el creador no adivina, letra válida).
- **Timer por deadline absoluto** (`SystemClock.elapsedRealtime`): si el
  host minimiza la app, al volver el turno vencido se procesa de inmediato.
- **Reconexión**: si un jugador se cae y vuelve con el mismo id, el host
  lo reasocia sin duplicarlo.
- **Sala llena / en curso**: el host rechaza con `ROOM_FULL` y desconecta.
- **Expulsión**: el anfitrión puede sacar jugadores desde la sala.
- **Puntuación real**: antes el `score` siempre era 0.
- Sin Firebase/Room/Retrofit: dependencias mínimas (Compose, Navigation,
  coroutines, play-services-nearby).

## Estructura

```
app/src/main/java/com/aistudio/ahorcado/
  MainActivity.kt, GameApp.kt      # entrada y navegación
  domain/model/Models.kt           # modelos (Kotlin puro)
  domain/logic/HangmanEngine.kt    # reglas del juego (Kotlin puro + tests)
  net/MessageProtocol.kt           # protocolo JSON
  net/NearbyLink.kt                # wrapper de Nearby Connections
  data/GameRepository.kt           # estado autoritativo, turnos, timer, puntos
  data/WordBank.kt                 # palabras por categoría
  ui/theme/Theme.kt                # paleta doodle del icono
  ui/components/                   # DoodleButton, fichas, muñeco en Canvas…
  ui/screens/                      # Menu, Lobby, Word, Game
```

## Icono

`res/drawable-nodpi/icono_ahorcado.png` es el icono original del juego y se
usa como icono del launcher (`android:icon`). Para un icono adaptativo HD,
re-exportarlo en 1024×1024 y generar los `mipmap-*` con Android Studio
(clic derecho en `res` → New → Image Asset).

# Atalaya — Gestor de contraseñas offline-first para Android

Aplicación nativa para Android desarrollada en Kotlin con Jetpack Compose como trabajo final de la asignatura **Programación de Aplicaciones Móviles Nativas (PAMN)** en la Universidad de Las Palmas de Gran Canaria (ULPGC).

## Autores
- **Álvaro García Suárez** (Integrante A)
- **Marcos González Gómez** (Integrante B)

Repositorio oficial: https://github.com/agsuarezz/AtalayaPAMN.git

---

## Objetivos y funcionalidades

Atalaya ofrece una bóveda de contraseñas **100 % local (offline-first)** con identidad visual retro de terminal/hacker de los años 90 y medidas reales de seguridad hardware y física:

- **Cifrado en reposo:** Room + cifrado por campo mediante `AES/GCM/NoPadding`, con clave de datos (`DEK`) protegida por `PBKDF2WithHmacSHA256` (contraseña maestra) y Android Keystore.
- **Biometría real:** Desbloqueo con `BiometricPrompt` ligado a una clave hardware del Keystore mediante `CryptoObject`.
- **Protección de sesión y pantalla:** `FLAG_SECURE`, limpieza de claves en memoria al bloquear, `allowBackup="false"`, portapapeles sensible con borrado automático a los 30 s.
- **Sensores físicos reactivos (`callbackFlow`):** Bloqueo inmediato al agitar el dispositivo (acelerómetro) o al ponerlo boca abajo / guardarlo en el bolsillo (proximidad/orientación), con degradación elegante si falta algún sensor.
- **Generador de contraseñas:** Consola interactiva con ajuste de longitud, conjuntos de caracteres y estimación de entropía.
- **Auditoría de filtraciones (HIBP):** Consulta bajo demanda mediante *k-anonymity* (solo se envían los 5 primeros caracteres del hash SHA-1).
- **Copia de seguridad cifrada:** Exportación e importación mediante Storage Access Framework (SAF).
- **Accesibilidad:** Contraste WCAG AA, áreas táctiles ≥ 48 dp, texto escalable (`sp`), etiquetas limpias para TalkBack y respeto al ajuste de animaciones reducidas.

> **Estado actual:** Paso 3 completado (Room `2.8.5` y repositorios cifrados con AAD por columna). Las capturas de pantalla se añadirán al completar las pantallas funcionales.

---

## Tecnologías y versiones principales

| Tecnología | Versión | Uso |
| :--- | :---: | :--- |
| Kotlin | `2.2.10` | Lenguaje principal |
| Android Gradle Plugin (AGP) | `9.5.2` | Sistema de construcción (`compileSdk` / `targetSdk 37`, `minSdk 26`) |
| Gradle | `9.6.0` | Wrapper y caché de configuración |
| Jetpack Compose BOM | `2026.02.01` | UI declarativa + Material 3 |
| Navigation Compose | `2.10.2` | Navegación entre pantallas |
| Dagger Hilt + KSP | `2.60.1` / `2.3.12` | Inyección de dependencias |
| Room | `2.8.5` | Persistencia SQLite local |
| AndroidX Lifecycle | `2.11.1` | ViewModels y observabilidad de ciclo de vida |
| Kotlinx Coroutines | `1.11.0` | Asincronía y `Flow` |
| JUnit / MockK / Turbine | `4.13.2` / `1.14.12` / `1.2.1` | Tests unitarios y de flujos |

---

## Arquitectura

La aplicación sigue **MVVM + Clean Architecture por capas** en un único módulo Gradle (`es.ulpgc.pamn.atalaya`):

- `ui/`: Pantallas Compose, componentes reutilizables retro (`RetroButton`, `RetroTextField`, `RetroCard`, `RetroTopBar`), tema Material 3 (`ui/theme`), navegación y `ViewModels` exponiendo `StateFlow<UiState>`.
- `domain/`: Modelos de dominio, interfaces de repositorio y casos de uso en Kotlin puro.
- `data/`: Persistencia local (Room, DataStore), cliente de red (HIBP) y gestor de copias cifradas (SAF).
- `core/`: Cifrado, Keystore, biometría, sensores (`callbackFlow`), portapapeles seguro y `DispatcherProvider` inyectable.
- `di/`: Módulos de Hilt.

Más detalle sobre requisitos, decisiones técnicas y trazabilidad en [`docs/`](docs/):
- [`docs/requisitos.md`](docs/requisitos.md)
- [`docs/decisiones.md`](docs/decisiones.md)
- [`docs/uso-ia.md`](docs/uso-ia.md)

---

## Configuración, compilación y tests

### Requisitos previos
- Android Studio actualizado con JDK 25 (gestionado mediante Gradle Toolchain / `gradle-daemon-jvm.properties`) y Android SDK con API 37.

### Comandos principales
```bash
# Ejecutar tests unitarios
./gradlew testDebugUnitTest

# Compilar APK de depuración
./gradlew assembleDebug
```

En Windows (PowerShell):
```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

---

## Servicios externos utilizados

- **Have I Been Pwned — Pwned Passwords API v3** (`https://api.pwnedpasswords.com/range/{prefix}`): usada opcionalmente bajo demanda del usuario mediante *k-anonymity* (solo envía 5 caracteres hexadecimales del SHA-1). La aplicación funciona por completo sin conexión cuando no se invoca esta comprobación.

---

## Licencias de librerías y recursos

- **Fuente tipográfica:** [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) — Licencia **SIL Open Font License 1.1 (OFL-1.1)**. Ficheros incluidos en `app/src/main/res/font/`.
- **Librerías AndroidX, Compose, Kotlin, Coroutines y Hilt:** Licencias **Apache License 2.0**.
- **MockK / Turbine / JUnit:** Apache 2.0 / Eclipse Public License 1.0.

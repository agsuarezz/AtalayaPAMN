# AGENTS.md — Gestor de contraseñas Android (PAMN, ULPGC)

Instrucciones permanentes para cualquier agente de IA que trabaje en este repositorio.

## Contexto

- Trabajo final de "Programación de Aplicaciones Móviles Nativas" (ULPGC). Equipo de **2 personas**.
- App: **gestor de contraseñas nativo para Android en Kotlin**, offline-first, con biometría y sensores del dispositivo.
- Se evalúa con rúbrica: funcionalidad y validación (3), arquitectura y calidad técnica (2), proceso y GitHub (1,5), memoria (1,5), UI/UX (1), presentación y defensa (1).
- **Cada integrante defenderá el código en persona.** No se acepta código que no podamos explicar. Prioriza claridad sobre ingenio y no añadas complejidad, pantallas o tecnologías sin utilidad: no puntúan.
- Idioma de la UI y de la documentación: español. Strings en `strings.xml` (nada hardcodeado).
- Requisitos y alcance: ver `docs/requisitos.md` (fuente de verdad). No los dupliques aquí.

## Stack y arquitectura

- Kotlin, Jetpack Compose + Material 3, Navigation Compose. Un solo módulo Gradle con paquetes claros.
- MVVM con capas `ui` / `domain` (casos de uso) / `data` (repositorios + fuentes de datos). Flujo unidireccional con `StateFlow` y `UiState` (Loading / Success / Error).
- Inyección de dependencias con Hilt. Dispatchers inyectables para poder testear.
- Persistencia: Room con datos sensibles cifrados (AES-GCM con clave del Android Keystore, o SQLCipher; la decisión se registra en `docs/decisiones.md`).
- Derivación de clave desde la contraseña maestra con Argon2 o PBKDF2 + sal aleatoria. **Nunca** se almacena la contraseña maestra.
- Asincronía: corrutinas + Flow. Cifrado, BD y red fuera del hilo principal.
- Sensores expuestos como `Flow` mediante una clase envoltorio (`callbackFlow` sobre `SensorManager`).
- Funciones nativas con propósito real:
  - Biometría (`androidx.biometric`) ligada a una clave del Keystore mediante `CryptoObject`, no un simple "sí/no" de UI.
  - Acelerómetro (agitar = bloqueo inmediato) y proximidad/orientación (boca abajo o en bolsillo = bloqueo). Configurables y con degradación elegante si el sensor no existe.
  - `FLAG_SECURE`, portapapeles marcado como sensible con borrado automático a los 30 s.
- Servicios externos: Have I Been Pwned (Pwned Passwords, k-anonymity: solo se envían 5 caracteres del hash SHA-1). Sincronización en la nube **fuera de alcance** salvo decisión expresa del equipo.
- minSdk 26 (revisable), targetSdk la última estable. **Verifica las versiones de librerías antes de fijarlas; no inventes dependencias ni métodos.**

## Estética de la UI: retro hacker de los 90

La app tiene identidad visual de terminal / hacker de los años 90. Se implementa con un tema propio de Material 3 en Compose, no con parches por pantalla.

- **Paleta:** fondo casi negro (`#0A0F0A` o similar) y texto en verde fósforo (`#00FF41`) como color principal; ámbar (`#FFB000`) como secundario o de aviso; rojo apagado para errores. Definir todos los colores en el tema (`Color.kt`, `Theme.kt`) y usarlos solo a través de `MaterialTheme`. Desactivar el color dinámico (Material You).
- **Tipografía:** monoespaciada en toda la app (por ejemplo JetBrains Mono, IBM Plex Mono o VT323, todas con licencia OFL). Incluir la fuente en `res/font/` y anotar su licencia en el README.
- **Formas y componentes:** esquinas rectas (`RectangleShape`), bordes finos de 1 dp en verde, sin sombras ni degradados. Botones y campos con aspecto de terminal, por ejemplo prefijo `>` o `$`, o texto entre corchetes `[ DESBLOQUEAR ]`. Cursor parpadeante en los campos de texto.
- **Detalles temáticos** (sutiles y baratos, sin convertirlo en un proyecto aparte): pantalla de arranque tipo boot con líneas de texto, mensajes de estado en formato log (`[OK] Bóveda desbloqueada`), generador de contraseñas con aspecto de consola. Efecto de líneas de escaneo opcional y muy ligero.
- **Límites obligatorios (la accesibilidad puntúa):**
  - Contraste mínimo WCAG AA (4,5:1) en todo el texto. Verde sobre negro lo cumple, pero verificar los tonos atenuados.
  - Las animaciones (cursor, boot) deben respetar el ajuste del sistema de animaciones reducidas y no afectar a la legibilidad ni a TalkBack.
  - Tamaños táctiles de al menos 48 dp y `contentDescription` descriptivo: el adorno visual (`>`, `[ ]`, ASCII art) no debe leerse en voz alta; marcarlo como no accesible o dar una descripción limpia.
  - Texto escalable (`sp`), sin romper el diseño con el tamaño de fuente grande del sistema.
- **Coherencia:** un único tema, componentes reutilizables (`RetroButton`, `RetroTextField`, `RetroCard`…) en un paquete `ui/components` o `ui/theme`. Cualquier pantalla nueva debe usarlos.
- **Documentación:** registrar la decisión de diseño y sus límites en `docs/decisiones.md`, y mostrar en la memoria el prototipo inicial frente al resultado final.
- **Recursos:** si se usa arte, iconos o fuentes de terceros, solo con licencia libre y con la fuente anotada en el README. No copiar logos ni estética de marcas o películas concretas.

## Seguridad (se defenderá)

- Ningún secreto, clave o credencial real en el repositorio. Usar `.gitignore`, `local.properties` y ficheros `.example`.
- No loguear datos sensibles (contraseñas, claves, contenido de credenciales).
- Desactivar backups de datos sensibles (`allowBackup` / `dataExtractionRules`).
- Bloquear al pasar a segundo plano y limpiar claves en memoria al bloquear.
- Mantener un modelo de amenazas breve en `docs/decisiones.md`: qué protegemos y qué **no**.

## Proceso de desarrollo (puntúa)

- `main` siempre estable. Trabajo en ramas `feature/*`, `fix/*`, `docs/*` con Pull Request y revisión por el otro compañero.
- Commits pequeños y descriptivos (Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`), referenciando el issue (`#n`).
- Issues para funcionalidades, tareas e incidencias. Hitos: 1) Análisis y diseño, 2) Implementación, 3) Pruebas, 4) Preparación de la entrega. Plantillas en `.github/`.
- Nunca subir trabajo "de golpe": incrementos pequeños y verificables.
- Entrega: tag/release `v1.0.0` + APK de demostración.
- README completo: descripción y objetivos, autores, funcionalidades, capturas, tecnologías con versiones, resumen de arquitectura, configuración/compilación/ejecución, servicios usados, licencias de librerías y recursos.
- Si no tienes acceso a GitHub, genera los comandos `git` y los textos de issues/PR para que los ejecutemos nosotros.

## Documentación continua (`/docs`)

Actualiza estos ficheros con cada cambio relevante, no al final:

- `docs/requisitos.md`: RF-01…, RNF-01… verificables + historias de usuario.
- `docs/decisiones.md`: ADR cortos (contexto, opciones, decisión, consecuencias) + modelo de amenazas.
- `docs/pruebas.md`: casos de prueba (condición, acción, esperado, obtenido), dispositivos/emuladores y versiones, errores detectados y correcciones.
- `docs/uso-ia.md`: **registro de todo uso de IA generativa** (herramienta, finalidad, parte del proyecto, prompt representativo, resultado, cómo lo revisamos y modificamos). La asignatura exige declararlo.
- `docs/memoria/`: un fichero por apartado de la memoria: 1 Introducción y objetivos, 2 Análisis de requisitos, 3 Interfaz y UX, 4 Arquitectura, 5 Datos y persistencia, 6 Desarrollo e implementación, 7 Pruebas y validación, 8 Proceso y gestión, 9 Uso de IA, 10 Conclusiones y trabajo futuro, más fuentes.

## Cómo debes trabajar

1. Antes de cambiar nada importante, explica en pocas líneas qué vas a hacer y por qué. Si hay alternativas razonables, compáralas brevemente y recomienda una.
2. Tras cada paso, resume en 3-5 líneas qué hace el código y **qué preguntas de defensa podría hacer el profesor** sobre él.
3. Escribe tests junto con el código: JUnit + MockK + Turbine para dominio, cifrado, generador y ViewModels; Compose Testing para el flujo principal. Ejecútalos antes de dar un paso por terminado.
4. Si algo no es necesario para los requisitos, dilo en lugar de añadirlo.
5. Si una decisión afecta a la arquitectura, los datos o la seguridad, regístrala en `docs/decisiones.md`.
6. Si hay ambigüedad en lo que se pide, pregunta antes de asumir.
7. Registra tu aportación en `docs/uso-ia.md` cuando el equipo te lo pida o al cerrar cada hito.

## Orden de implementación previsto

1. Esqueleto Gradle + Hilt + Compose + navegación + CI básico (GitHub Actions: build y tests).
2. Capa de cifrado y Keystore, con tests.
3. Room + repositorio.
4. Alta de contraseña maestra y desbloqueo con biometría.
5. CRUD de credenciales y lista con búsqueda/filtros.
6. Generador de contraseñas.
7. Sensores y autobloqueo.
8. Ajustes, comprobación HIBP, exportar/importar copia cifrada.
9. Pulido de UI, accesibilidad (contentDescription, tamaños táctiles, contraste, TalkBack) y tests finales.

## Definición de "hecho"

- Compila desde cero siguiendo el README y funciona sin conexión.
- Recorrido completo: crear maestra → desbloquear con huella → crear/editar/borrar credencial → copiar → autobloqueo por sensor.
- Errores visibles y tratados: biometría no disponible o cancelada, sensor ausente, sin red en HIBP, BD corrupta, contraseña maestra incorrecta.
- Tests pasando en CI y documentados en `docs/pruebas.md`.

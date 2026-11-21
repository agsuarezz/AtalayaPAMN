# Requisitos del sistema — Atalaya

**Proyecto:** Atalaya — Gestor de contraseñas offline-first para Android  
**Asignatura:** Programación de Aplicaciones Móviles Nativas (PAMN) — ULPGC  
**Equipo:** Álvaro García Suárez (Integrante A) y Marcos González Gómez (Integrante B)  
**Repositorio:** https://github.com/agsuarezz/AtalayaPAMN.git  
**Alcance:** Aplicación nativa Android en Kotlin, offline-first, con biometría ligada a clave hardware y sensores físicos de seguridad. La sincronización en la nube queda fuera de alcance para garantizar el control total de los datos en local.

---

## 1. Requisitos Funcionales (RF)

| ID | Título | Descripción verificable | Hito |
| :--- | :--- | :--- | :---: |
| **RF-01** | Alta de contraseña maestra | En el primer arranque, el usuario debe definir una contraseña maestra de al menos 10 caracteres con confirmación e indicador de fortaleza. La contraseña maestra nunca se almacena en claro ni se registra en logs. | H2 |
| **RF-02** | Desbloqueo con contraseña maestra | El usuario puede desbloquear la bóveda introduciendo la contraseña maestra; se deriva la clave con `PBKDF2WithHmacSHA256` y sal aleatoria. Si la contraseña es incorrecta, se muestra un error claro sin revelar información interna. | H2 |
| **RF-03** | Desbloqueo biométrico con Keystore | Si el usuario activa la biometría y el dispositivo dispone de hardware compatible, puede desbloquear mediante `BiometricPrompt` autenticado con `CryptoObject` ligado a una clave del Android Keystore. Si la biometría no está disponible o se cancela, la app degrada de forma elegante al acceso por contraseña maestra. | H2 |
| **RF-04** | Listado, búsqueda y filtros | La pantalla principal de bóveda muestra las credenciales guardadas con búsqueda en tiempo real por título/usuario/URL y filtrado por categoría. | H2 |
| **RF-05** | CRUD de credenciales | El usuario puede crear, consultar en detalle (con opción de mostrar/ocultar la contraseña), editar y eliminar (previa confirmación) credenciales con campos de título, usuario, contraseña, URL, categoría y notas. | H2 |
| **RF-06** | Copia segura al portapapeles | Al copiar una contraseña o usuario sensible, el dato se marca con `ClipDescription.EXTRA_IS_SENSITIVE` (Android 13+) y se programa su limpieza automática del portapapeles a los 30 segundos. | H2 |
| **RF-07** | Generador de contraseñas | Consola generadora con longitud configurable, selección de conjuntos (mayúsculas, minúsculas, dígitos, símbolos), estimación de entropía/fortaleza y acción para copiar o aplicar directamente al formulario de credencial. | H2 |
| **RF-08** | Bloqueo por agitado (acelerómetro) | Si la opción está habilitada y existe acelerómetro, agitar el dispositivo bloquea inmediatamente la bóveda y limpia las claves en memoria. Si el sensor no existe, la opción se informa como no disponible sin fallos. | H2 |
| **RF-09** | Bloqueo boca abajo / bolsillo | Si la opción está habilitada y los sensores están disponibles, colocar el teléfono boca abajo o cubrir el sensor de proximidad/orientación bloquea la bóveda inmediatamente. Si algún sensor no existe, se degrada con elegancia. | H2 |
| **RF-10** | Autobloqueo en segundo plano | Al pasar la aplicación a segundo plano o tras el tiempo de inactividad configurado en ajustes, la bóveda se bloquea automáticamente y borra las claves de sesión de memoria. | H2 |
| **RF-11** | Ajustes y cambio de maestra | Pantalla de ajustes para activar/desactivar biometría, sensores de bloqueo, tiempo de autobloqueo, efecto visual *scanlines* y cambio seguro de contraseña maestra (re-cifrando la clave de datos `DEK`). | H2 |
| **RF-12** | Comprobación HIBP (k-anonymity) | Bajo demanda del usuario, verifica si una contraseña aparece en filtraciones públicas usando la API de Pwned Passwords enviando exclusivamente los 5 primeros caracteres del hash SHA-1 en hexadecimal. Maneja explícitamente el estado sin red. | H2 |
| **RF-13** | Copia de seguridad cifrada (SAF) | Permite exportar e importar la bóveda en un fichero JSON cifrado con clave derivada mediante Storage Access Framework (SAF), verificando integridad y contraseña al importar. | H2 |
| **RF-14** | Pantalla de arranque retro (*boot*) | Breve secuencia inicial con líneas de estado en formato log (`[OK] ...`) al abrir la app. Si el dispositivo tiene desactivadas las animaciones del sistema, se omite automáticamente pasando directo al desbloqueo. | H1 / H2 |

---

## 2. Requisitos No Funcionales (RNF)

| ID | Categoría | Descripción verificable |
| :--- | :--- | :--- |
| **RNF-01** | Seguridad en reposo | Persistencia en Room con cifrado por campo mediante `AES/GCM/NoPadding` usando una `DEK` en memoria protegida por `PBKDF2WithHmacSHA256` (maestra) y Android Keystore (biometría). Al bloquear, la clave de sesión se limpia de memoria. |
| **RNF-02** | Prevención de fugas | `FLAG_SECURE` activo para impedir capturas y previsualización en apps recientes; `android:allowBackup="false"` y `dataExtractionRules` restrictivas; prohibición absoluta de loguear contraseñas, claves o secretos. |
| **RNF-03** | Offline-first | Toda la funcionalidad principal funciona al 100 % sin conexión a Internet; únicamente la consulta bajo demanda a HIBP (`RF-12`) requiere red. |
| **RNF-04** | Compatibilidad | `minSdk = 26` (Android 8.0), `targetSdk = 37`, probado en dispositivo físico Android 15 (API 35) con biometría y sensores completos y en emuladores. |
| **RNF-05** | Arquitectura y calidad | Arquitectura MVVM por capas (`ui` / `domain` / `data` / `core`), inyección de dependencias con Hilt, `StateFlow<UiState>`, corrutinas con `DispatcherProvider` inyectable y tests automatizados en CI (JUnit, MockK, Turbine, Compose Testing). |
| **RNF-06** | Accesibilidad | Contraste WCAG AA (≥ 4,5:1) en todo el texto, áreas táctiles mínimas de `48.dp`, tipografía escalable en `sp`, `contentDescription` limpio ocultando adornos ASCII (`>`, `[ ]`), respeto a animaciones reducidas y validación con TalkBack. |
| **RNF-07** | Rendimiento | Toda operación criptográfica, derivación KDF, lectura/escritura en Room/DataStore y petición de red se ejecuta fuera del hilo principal (`Dispatchers.Default` / `Dispatchers.IO`). |
| **RNF-08** | Idioma y mantenibilidad | Interfaz y documentación íntegramente en español. Todos los textos visibles externalizados en `strings.xml` sin cadenas hardcodeadas. |
| **RNF-09** | Identidad visual | Tema propio Material 3 retro hacker de los años 90 (fondo `#0A0F0A`, verde fósforo `#00FF41`, ámbar `#FFB000`, esquinas `RectangleShape`, bordes de `1.dp`, fuente monoespaciada JetBrains Mono OFL y Material You desactivado). |
| **RNF-10** | Licencias y trazabilidad | Toda librería y recurso de terceros tiene licencia libre documentada en `README.md` y todo uso de IA generativa queda registrado en `docs/uso-ia.md`. |

---

## 3. Historias de usuario iniciales

| ID | Como… | Quiero… | Para… | RF asociados | Responsable |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **HU-01** | Usuario nuevo | Crear una contraseña maestra robusta la primera vez que abro Atalaya | Proteger mi bóveda local sin depender de servidores externos | RF-01, RF-14 | Álvaro García Suárez |
| **HU-02** | Usuario recurrente | Desbloquear la bóveda con mi huella o contraseña maestra | Acceder rápido de forma segura mediante clave hardware del Keystore | RF-02, RF-03 | Álvaro García Suárez |
| **HU-03** | Usuario de la bóveda | Guardar, buscar, filtrar, editar y borrar mis credenciales | Mantener mis accesos organizados y localizables al instante | RF-04, RF-05 | Marcos González Gómez |
| **HU-04** | Usuario preocupado por la privacidad | Copiar contraseñas sin dejar rastro en el portapapeles ni en capturas | Evitar que otras apps o miradas indiscretas lean mis secretos | RF-06, RNF-02 | Marcos González Gómez |
| **HU-05** | Usuario que crea cuentas | Generar contraseñas aleatorias largas y comprobar su fortaleza | Evitar reutilizar claves débiles | RF-07 | Álvaro García Suárez |
| **HU-06** | Usuario en movimiento | Que la app se bloquee si agito el móvil, lo pongo boca abajo o cambio de app | Proteger la bóveda ante un descuido o robo con el teléfono desbloqueado | RF-08, RF-09, RF-10 | Marcos González Gómez |
| **HU-07** | Usuario avanzado | Verificar si una contraseña está filtrada en HIBP sin exponerla | Saber si debo cambiar una clave comprometida | RF-12 | Marcos González Gómez |
| **HU-08** | Usuario que cambia de móvil | Configurar mis medidas de seguridad y exportar/importar copias cifradas | Conservar mis credenciales sin perder seguridad ni depender de la nube | RF-11, RF-13 | Álvaro García Suárez |

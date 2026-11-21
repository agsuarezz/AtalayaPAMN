# Decisiones técnicas y modelo de amenazas — Atalaya

Registro de decisiones de arquitectura (ADR) y análisis de amenazas del proyecto **Atalaya** (PAMN — ULPGC).

---

## ADR-001: Persistencia cifrada con Room + AES-GCM por campo y derivación PBKDF2

- **Estado:** Aceptada  
- **Contexto:** Atalaya debe almacenar credenciales offline en Android (API 26+) con desbloqueo por contraseña maestra y por biometría ligada a Android Keystore mediante `CryptoObject`.
- **Opciones evaluadas:**
  1. **Opción A — Room estándar + cifrado por campo con AES-GCM (`AES/GCM/NoPadding`)**: una clave de datos (`DEK`) cifra los campos sensibles de cada registro con IV único por cifrado. La `DEK` se guarda envuelta (cifrada) por dos vías independientes: una clave derivada de la contraseña maestra (`PBKDF2WithHmacSHA256` + sal aleatoria) y, si el usuario activa la biometría, una clave simétrica protegida en Android Keystore que requiere autenticación biométrica (`CryptoObject`).
  2. **Opción B — Room + SQLCipher**: cifra el fichero SQLite completo (incluidos nombres de tablas e índices), pero introduce dependencias binarias nativas (`.so`), aumenta el tamaño del APK y complica la integración directa con `BiometricPrompt` + `CryptoObject`.
- **Decisión:** Se elige la **Opción A (Room + cifrado por campo con AES-GCM)** y derivación con **`PBKDF2WithHmacSHA256`** nativo en Android (API 26+).
- **Parámetros criptográficos fijados (Paso 2):**
  - **Derivación (KDF):** `PBKDF2WithHmacSHA256`, sal aleatoria (`SecureRandom`) de 16 bytes (128 bits), `310.000` iteraciones por defecto (alineado con recomendación OWASP), clave derivada de 256 bits. La entrada se recibe en `CharArray` y se limpia en `finally` con `PBEKeySpec.clearPassword()`.
  - **Clave de datos (`DEK`) y cifrado simétrico:** `DEK` aleatoria de 256 bits (`32` bytes), cifrado `AES/GCM/NoPadding` con IV aleatorio de 12 bytes (96 bits) en cada cifrado, tag de autenticación de 128 bits y soporte de AAD.
  - **Keystore biométrico (`AndroidKeyStore`):** clave AES-256 hardware con `setUserAuthenticationRequired(true)`, `AUTH_BIOMETRIC_STRONG` en API 30+ (y `-1` en API 26–29) y `setInvalidatedByBiometricEnrollment(true)` para revocar la clave si se añaden o cambian huellas del sistema.
  - **Gestión en memoria (`VaultSessionManager`):** copia defensiva de la `DEK` en sesión y borrado explícito con ceros (`fill(0)`) al bloquear o al finalizar `withDek`.
- **Consecuencias:**
  - *Positivas:* Arquitectura 100 % explicable y defendible sin cajas negras nativas; integración limpia con `BiometricPrompt.CryptoObject`; permite cambiar la contraseña maestra re-cifrando únicamente la `DEK` envuelta sin re-cifrar toda la base de datos.
  - *Negativas:* Los metadatos estructurales de SQLite (número aproximado de filas o marcas temporales no cifradas si las hubiera) no están cifrados; se mitiga cifrando todo campo identificativo o sensible (título, usuario, contraseña, URL, notas) y bloqueando backups del sistema.

---

## ADR-002: Identidad visual retro años 90 en Material 3 y límites de accesibilidad

- **Estado:** Aceptada  
- **Contexto:** La aplicación requiere una identidad visual de terminal/hacker de los años 90 manteniendo usabilidad real y cumplimiento estricto de accesibilidad móvil (WCAG AA y TalkBack).
- **Opciones evaluadas:**
  1. Tema oscuro Material 3 personalizado con paleta fija (`#0A0F0A` de fondo, `#00FF41` verde fósforo principal, `#FFB000` ámbar de aviso, rojo `#FF5252` para errores), `RectangleShape`, bordes de `1.dp`, fuente monoespaciada **JetBrains Mono** (licencia SIL OFL 1.1) y Material You desactivado.
  2. Estilizado ad-hoc por pantalla o tipografías pixeladas tipo CRT (p. ej. VT323) difíciles de leer con escalado grande del sistema.
- **Decisión:** Se adopta la **Opción 1** centralizada en `ui/theme/` y `ui/components/` usando **JetBrains Mono** (SIL OFL 1.1), pantalla inicial *boot* breve y efecto *scanlines* opcional sutil.
- **Consecuencias:**
  - *Positivas:* Contraste superior a 4,5:1 garantizado, lectura clara incluso con tamaño de texto ampliado (`sp`), botones de al menos `48.dp` y adornos visuales (`>`, `[ ]`) ocultos o sustituidos en TalkBack mediante `semantics`.
  - *Compromiso obligatorio:* La pantalla *boot* y cualquier animación se saltan o desactivan automáticamente cuando `ANIMATOR_DURATION_SCALE == 0f` en el sistema.

---

## ADR-003: Alcance v1.0.0 offline-first con HIBP k-anonymity y backup cifrado SAF

- **Estado:** Aceptada  
- **Contexto:** El proyecto debe funcionar completamente sin conexión, pero aportar valor real frente a contraseñas comprometidas y cambio de dispositivo sin introducir sincronización en la nube.
- **Decisión:** Incluir en la `v1.0.0`:
  1. **Have I Been Pwned (Pwned Passwords)** mediante modelo *k-anonymity*: el cliente calcula SHA-1 localmente y solo envía los primeros 5 caracteres del hash por HTTPS; la comprobación se lanza bajo demanda del usuario y falla de forma segura si no hay conexión.
  2. **Copia de seguridad cifrada local mediante Storage Access Framework (SAF)**: el usuario elige dónde guardar/abrir un archivo JSON cifrado con una clave derivada, evitando permisos amplios de almacenamiento y sin nube.

---

## Modelo de amenazas breve

### Qué SÍ protege Atalaya
- **Robo o pérdida física del terminal bloqueado:** la base de datos Room solo almacena campos cifrados con AES-GCM; sin la contraseña maestra ni la autenticación biométrica ligada al Keystore no es posible descifrar la `DEK`.
- **Extracción de copia de seguridad o inspección de archivos de la app:** `android:allowBackup="false"` y `dataExtractionRules` excluyen bases de datos y preferencias de copias en la nube o transferencias.
- **Capturas de pantalla, grabación o vista de aplicaciones recientes:** `FLAG_SECURE` bloquea las capturas y oculta la miniatura en el selector de tareas recientes.
- **Fugas a través del portapapeles:** al copiar contraseñas se etiqueta el contenido como sensible (`EXTRA_IS_SENSITIVE`) y se limpia automáticamente tras 30 segundos.
- **Acceso casual con el teléfono desbloqueado en la mano:** el bloqueo al pasar a segundo plano y los sensores configurables (agitar, boca abajo/proximidad) cierran la bóveda y limpian las claves de memoria.
- **Exposición de contraseñas al verificar filtraciones:** HIBP recibe únicamente un prefijo de 5 caracteres hexadecimales del SHA-1 (k-anonymity), nunca la contraseña ni el hash completo.

### Qué NO protege Atalaya
- **Dispositivo rooteado o comprometido con la bóveda abierta:** un atacante con privilegios `root`, depuración activa o acceso físico mientras la `DEK` está desbloqueada en memoria podría inspeccionar la RAM del proceso.
- **Malware con permisos abusivos de Accesibilidad o keyloggers:** si el teclado o un servicio malicioso externo captura lo que el usuario teclea al introducir la contraseña maestra.
- **Contraseña maestra débil o compartida:** si el usuario escoge una contraseña corta o predecible frente a un ataque de fuerza bruta offline sobre el fichero cifrado o la BD extraída.
- **Olvido de la contraseña maestra sin copia ni biometría:** al no existir servidor ni recuperación remota, la pérdida de la contraseña maestra hace irrecuperables los datos cifrados.

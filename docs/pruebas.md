# Plan y registro de pruebas — Atalaya

Documento vivo de verificación funcional, criptográfica, de accesibilidad y en dispositivo físico (**PAMN — ULPGC**).

---

## 1. Entorno de pruebas

| Tipo | Entorno / Herramienta | Detalles |
| :--- | :--- | :--- |
| **Tests unitarios JVM** | JUnit 4 + Turbine + MockK + `kotlinx-coroutines-test` | Ejecutados en local (`./gradlew.bat testDebugUnitTest`) y en CI (GitHub Actions Ubuntu + JDK 25). |
| **Dispositivo físico** | Móvil Android 15 (API 35) | Biometría hardware, acelerómetro, sensor de proximidad/orientación y TalkBack (pruebas de campo en Hitos 2 y 3). |
| **Emuladores** | Android Emulator API 26 / API 35+ | Validación de `minSdk = 26` y degradación ante ausencia de hardware biométrico o sensores. |

---

## 2. Casos de prueba automatizados (Unit Tests)

### Paso 1 — Esqueleto base, Dispatchers, UiState y Navegación
| ID | Caso | Condición | Acción | Resultado esperado | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **UT-01** | `DispatcherProviderTest` | Instancia de `DefaultDispatcherProvider` | Consultar `main`, `io`, `default` | Coinciden con `Dispatchers.Main`, `IO` y `Default` | Pasado (1/1) |
| **UT-02** | `AtalayaDestinationTest` | Enum `AtalayaDestination` | Inspeccionar rutas | Todas son no vacías y sin duplicados | Pasado (1/1) |
| **UT-03** | `UiStateTest` | `MutableStateFlow<UiState<String>>` | Emitir `Loading` → `Success` → `Error` | Turbine recibe los 3 estados en orden exacto | Pasado (1/1) |

### Paso 2 — Capa de cifrado, Keystore y sesión
| ID | Caso | Condición | Acción | Resultado esperado | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **UT-04** | `EncryptedPayloadTest` | Payloads válidos y corruptos | Serializar/deserializar `v1:iv:ct` y llamar `toString()` | Round-trip exacto; formatos inválidos lanzan `CryptoOperationException`; `toString()` no filtra bytes | Pasado (4/4) |
| **UT-05** | `Pbkdf2KeyDerivationTest` | Contraseña en `CharArray`, sal 16B | Derivar con 1.000 y 310.000 iteraciones y probar entradas inválidas | Clave determinista de 32B, distinta por sal/clave; entradas inseguras rechazadas | Pasado (5/5) |
| **UT-06** | `DefaultAesGcmCipherTest` | Claves de 32B, texto Unicode y AAD | Cifrar, descifrar, alterar 1 bit en IV/ciphertext/AAD o usar clave errónea | Round-trip correcto, IV único por cifrado y `AuthenticationFailedException` ante manipulación | Pasado (6/6) |
| **UT-07** | `VaultSessionManagerTest` | Sesión inicial bloqueada | Observar `isUnlocked`, llamar `unlock`, `withDek` y `lock` | `false -> true -> false`; `withDek` limpia copia temporal; tras `lock` lanza `VaultLockedException` | Pasado (4/4) |
| **UT-08** | `AndroidKeystoreManagerCipherOpsTest` | `Cipher` AES/GCM/NoPadding real | Envolver/desenvolver `DEK` y manipular ciphertext | `DEK` recuperada idéntica; manipulación lanza `AuthenticationFailedException` | Pasado (2/2) |

### Paso 3 — Room y repositorios cifrados
| ID | Caso | Condición | Acción | Resultado esperado | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **UT-09** | `CredentialCryptoMapperTest` | Credencial completa con Unicode | Mapear `toEntity` / `toDomain`, intercambiar columnas y llamar `toString()` | Ningún texto en claro en entidad; intercambio lanza `AuthenticationFailedException`; `toString()` sin secretos | Pasado (3/3) |
| **UT-10** | `VaultRepositoryImplTest` | DAO mockeado + `VaultSessionManager` real | Guardar/leer con bóveda cerrada y abierta; observar `observeCredentials()` con Turbine | Bloqueada lanza `VaultLockedException` sin tocar DAO; `observeCredentials()` emite `[] -> [credencial] -> []` | Pasado (4/4) |
| **UT-11** | `VaultMetaRepositoryImplTest` | `VaultMetaDao` mockeado | Consultar inicialización, guardar/leer metadatos, actualizar biometría y `toString()` | Delegación correcta en `dispatchers.io` y `toString()` sin sal ni claves | Pasado (3/3) |

---

## 3. Pruebas manuales y en dispositivo físico (pendientes de Hito 2 / Hito 3)

| ID | Área | Condición | Acción | Resultado esperado | Resultado obtenido |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **PT-01** | Keystore + Biometría (Android 15) | Huella registrada en el teléfono | Activar biometría, añadir una nueva huella en Ajustes y volver a desbloquear | `KeyPermanentlyInvalidatedException` capturada y degradación a contraseña maestra | *Pendiente (Paso 4)* |
| **PT-02** | Sensores físicos (Android 15) | Bóveda desbloqueada | Agitar dispositivo / colocar boca abajo | Bloqueo inmediato y limpieza de `DEK` en memoria | *Pendiente (Paso 7)* |
| **PT-03** | Accesibilidad TalkBack | TalkBack activo en Android 15 | Recorrer pantallas principales | Anuncio limpio sin leer `>` ni `[ ]`, foco correcto, áreas ≥ 48 dp | *Pendiente (Paso 9)* |

---

## 4. Registro de incidencias detectadas y correcciones

- **INC-01 (Paso 1):** `Shape.kt` no compilaba con Material 3 de Compose BOM `2026.02.01` porque el constructor de 8 argumentos de `Shapes` es `internal` y `RectangleShape` no implementa `CornerBasedShape`. **Corrección:** sustituido por `RoundedCornerShape(0.dp)` en los 5 argumentos públicos de `Shapes`.

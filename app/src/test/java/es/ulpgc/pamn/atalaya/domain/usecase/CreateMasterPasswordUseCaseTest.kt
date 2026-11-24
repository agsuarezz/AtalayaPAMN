package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.DefaultAesGcmCipher
import es.ulpgc.pamn.atalaya.core.security.EncryptedPayload
import es.ulpgc.pamn.atalaya.core.security.KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.Pbkdf2KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.domain.model.VaultMetadata
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateMasterPasswordUseCaseTest {

    private val keyDerivation = Pbkdf2KeyDerivation()
    private val aesGcmCipher = DefaultAesGcmCipher()
    private val sessionManager = VaultSessionManager()

    private fun testDispatchers(dispatcher: CoroutineDispatcher): DispatcherProvider =
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
        }

    @Test
    fun `rechaza contrasena menor a 10 caracteres y limpia el CharArray`() = runTest {
        val repo = mockk<VaultMetaRepository>()
        val useCase = CreateMasterPasswordUseCase(
            vaultMetaRepository = repo,
            keyDerivation = keyDerivation,
            aesGcmCipher = aesGcmCipher,
            vaultSessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val chars = "corta123".toCharArray()

        val result = useCase(chars)

        assertTrue(result.isFailure)
        assertFalse(sessionManager.isUnlocked.value)
        assertTrue(chars.all { it == '\u0000' })
        coVerify(exactly = 0) { repo.saveMetadata(any()) }
    }

    @Test
    fun `rechaza inicializacion si la boveda ya existe`() = runTest {
        val repo = mockk<VaultMetaRepository>()
        coEvery { repo.isVaultInitialized() } returns true
        val useCase = CreateMasterPasswordUseCase(
            vaultMetaRepository = repo,
            keyDerivation = keyDerivation,
            aesGcmCipher = aesGcmCipher,
            vaultSessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val chars = "MiClaveSegura#2026".toCharArray()

        val result = useCase(chars)

        assertTrue(result.isFailure)
        assertFalse(sessionManager.isUnlocked.value)
        assertTrue(chars.all { it == '\u0000' })
        coVerify(exactly = 0) { repo.saveMetadata(any()) }
    }

    @Test
    fun `crea metadatos validos con PBKDF2 310000 envuelve DEK y abre sesion`() = runTest {
        val repo = mockk<VaultMetaRepository>()
        coEvery { repo.isVaultInitialized() } returns false
        val metadataSlot = slot<VaultMetadata>()
        coEvery { repo.saveMetadata(capture(metadataSlot)) } returns Unit

        val useCase = CreateMasterPasswordUseCase(
            vaultMetaRepository = repo,
            keyDerivation = keyDerivation,
            aesGcmCipher = aesGcmCipher,
            vaultSessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val rawPassword = "Atalaya#ClaveMaestra2026"
        val chars = rawPassword.toCharArray()

        val result = useCase(chars, nowEpochMillis = 123456L)

        assertTrue(result.isSuccess)
        assertTrue(sessionManager.isUnlocked.value)
        assertTrue(chars.all { it == '\u0000' })

        val saved = metadataSlot.captured
        assertEquals(KeyDerivation.DEFAULT_ITERATIONS, saved.kdfIterations)
        assertNull(saved.biometricWrappedDek)
        assertEquals(123456L, saved.createdAtEpochMillis)
        assertEquals(123456L, saved.updatedAtEpochMillis)

        val salt = Base64.getDecoder().decode(saved.saltBase64)
        assertEquals(KeyDerivation.DEFAULT_SALT_SIZE, salt.size)

        val kek = keyDerivation.deriveKey(rawPassword.toCharArray(), salt, saved.kdfIterations)
        val unwrappedDek = aesGcmCipher.decrypt(
            payload = EncryptedPayload.fromStorageString(saved.masterWrappedDek),
            key = kek,
            associatedData = MASTER_WRAP_AAD
        )
        sessionManager.withDek { activeDek ->
            assertArrayEquals(unwrappedDek, activeDek)
        }
        kek.fill(0)
        unwrappedDek.fill(0)
        sessionManager.lock()
    }
}

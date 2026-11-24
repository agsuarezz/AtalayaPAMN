package es.ulpgc.pamn.atalaya.data.repository

import app.cash.turbine.test
import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.DefaultAesGcmCipher
import es.ulpgc.pamn.atalaya.core.security.Pbkdf2KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.data.local.dao.CredentialDao
import es.ulpgc.pamn.atalaya.data.local.entity.CredentialEntity
import es.ulpgc.pamn.atalaya.domain.model.Credential
import es.ulpgc.pamn.atalaya.domain.model.CredentialCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultRepositoryImplTest {

    private val keyDerivation = Pbkdf2KeyDerivation()
    private val mapper = CredentialCryptoMapper(DefaultAesGcmCipher())
    private val sessionManager = VaultSessionManager()

    private fun testDispatchers(dispatcher: CoroutineDispatcher): DispatcherProvider {
        return object : DispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
        }
    }

    @Test
    fun `saveCredential cifra la credencial antes de llamar al DAO y retorna el id`() = runTest {
        val dao = mockk<CredentialDao>()
        val repository = VaultRepositoryImpl(
            credentialDao = dao,
            mapper = mapper,
            sessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val dek = keyDerivation.generateRandomKey()
        sessionManager.unlock(dek)

        val entitySlot = slot<CredentialEntity>()
        coEvery { dao.upsert(capture(entitySlot)) } returns 42L

        val credential = Credential(
            title = "GitHub",
            username = "agsuarezz",
            password = "MiPassword#2026",
            url = "https://github.com",
            notes = "Cuenta ULPGC",
            category = CredentialCategory.WORK,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 2000L
        )

        val savedId = repository.saveCredential(credential)

        assertEquals(42L, savedId)
        assertFalse(entitySlot.captured.encryptedPassword.contains("MiPassword#2026"))
        assertFalse(entitySlot.captured.encryptedTitle.contains("GitHub"))
        assertEquals(credential, mapper.toDomain(entitySlot.captured, dek).copy(id = 0L))
    }

    @Test
    fun `operaciones de escritura y lectura lanzan VaultLockedException sin tocar el DAO cuando esta bloqueada`() = runTest {
        val dao = mockk<CredentialDao>(relaxed = true)
        val repository = VaultRepositoryImpl(
            credentialDao = dao,
            mapper = mapper,
            sessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val sample = Credential(
            title = "Test",
            username = "user",
            password = "pwd",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L
        )

        assertThrowsSuspend<CryptoException.VaultLockedException> { repository.saveCredential(sample) }
        assertThrowsSuspend<CryptoException.VaultLockedException> { repository.getCredentialById(1L) }
        assertThrowsSuspend<CryptoException.VaultLockedException> { repository.deleteCredential(1L) }
        assertThrowsSuspend<CryptoException.VaultLockedException> { repository.getAllCredentialsSnapshot() }

        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { dao.getById(any()) }
        coVerify(exactly = 0) { dao.deleteById(any()) }
        coVerify(exactly = 0) { dao.getAll() }
    }

    @Test
    fun `observeCredentials emite vacio cuando esta bloqueada y descifra al desbloquear`() = runTest {
        val dao = mockk<CredentialDao>()
        val daoFlow = MutableStateFlow<List<CredentialEntity>>(emptyList())
        every { dao.observeAll() } returns daoFlow

        val repository = VaultRepositoryImpl(
            credentialDao = dao,
            mapper = mapper,
            sessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )

        val dek = keyDerivation.generateRandomKey()
        val credential = Credential(
            id = 5L,
            title = "Campus Virtual",
            username = "marcos.gonzalez",
            password = "PassCampus!88",
            category = CredentialCategory.EMAIL,
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L
        )
        daoFlow.value = listOf(mapper.toEntity(credential, dek))

        repository.observeCredentials().test {
            assertEquals(emptyList<Credential>(), awaitItem())

            sessionManager.unlock(dek)
            assertEquals(listOf(credential), awaitItem())

            sessionManager.lock()
            assertEquals(emptyList<Credential>(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCredentialById descifra registro existente o devuelve null si no existe`() = runTest {
        val dao = mockk<CredentialDao>()
        val repository = VaultRepositoryImpl(
            credentialDao = dao,
            mapper = mapper,
            sessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val dek = keyDerivation.generateRandomKey()
        sessionManager.unlock(dek)

        val expected = Credential(
            id = 9L,
            title = "WiFi Casa",
            username = "router",
            password = "WifiPassword#99",
            category = CredentialCategory.WIFI,
            createdAtEpochMillis = 10L,
            updatedAtEpochMillis = 20L
        )
        coEvery { dao.getById(9L) } returns mapper.toEntity(expected, dek)
        coEvery { dao.getById(999L) } returns null

        assertEquals(expected, repository.getCredentialById(9L))
        assertNull(repository.getCredentialById(999L))
    }

    private suspend inline fun <reified T : Throwable> assertThrowsSuspend(
        crossinline block: suspend () -> Unit
    ) {
        var thrown = false
        try {
            block()
        } catch (e: Throwable) {
            assertTrue(
                "Se esperaba ${T::class.java.simpleName}, pero se lanzó ${e::class.java.simpleName}",
                e is T
            )
            thrown = true
        }
        assertTrue("Se esperaba ${T::class.java.simpleName}, pero no se lanzó excepción", thrown)
    }
}

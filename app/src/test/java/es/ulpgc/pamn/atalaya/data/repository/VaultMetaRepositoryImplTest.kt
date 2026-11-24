package es.ulpgc.pamn.atalaya.data.repository

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.data.local.dao.VaultMetaDao
import es.ulpgc.pamn.atalaya.data.local.entity.VaultMetaEntity
import es.ulpgc.pamn.atalaya.domain.model.VaultMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultMetaRepositoryImplTest {

    private fun testDispatchers(dispatcher: CoroutineDispatcher): DispatcherProvider {
        return object : DispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
        }
    }

    @Test
    fun `isVaultInitialized refleja countMetadata del DAO`() = runTest {
        val dao = mockk<VaultMetaDao>()
        val repository = VaultMetaRepositoryImpl(
            vaultMetaDao = dao,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )

        coEvery { dao.countMetadata() } returns 0
        assertFalse(repository.isVaultInitialized())

        coEvery { dao.countMetadata() } returns 1
        assertTrue(repository.isVaultInitialized())
    }

    @Test
    fun `saveMetadata getMetadata updateBiometricWrappedDek y clearMetadata delegan correctamente`() = runTest {
        val dao = mockk<VaultMetaDao>(relaxed = true)
        val repository = VaultMetaRepositoryImpl(
            vaultMetaDao = dao,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val metadata = VaultMetadata(
            saltBase64 = "c2FsdF9zZWNyZXRhXzE2Qg==",
            kdfIterations = 310_000,
            masterWrappedDek = "v1:ivMaster:ctMaster",
            biometricWrappedDek = "v1:ivBio:ctBio",
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L
        )
        val entity = VaultMetaEntity(
            id = VaultMetaEntity.SINGLETON_ID,
            saltBase64 = metadata.saltBase64,
            kdfIterations = metadata.kdfIterations,
            masterWrappedDek = metadata.masterWrappedDek,
            biometricWrappedDek = metadata.biometricWrappedDek,
            createdAtEpochMillis = metadata.createdAtEpochMillis,
            updatedAtEpochMillis = metadata.updatedAtEpochMillis
        )

        coEvery { dao.getMetadata() } returns entity
        repository.saveMetadata(metadata)
        coVerify(exactly = 1) { dao.upsert(entity) }

        assertEquals(metadata, repository.getMetadata())

        repository.updateBiometricWrappedDek(null, 500L)
        coVerify(exactly = 1) { dao.updateBiometricWrappedDek(null, 500L) }

        repository.clearMetadata()
        coVerify(exactly = 1) { dao.deleteAll() }

        coEvery { dao.getMetadata() } returns null
        assertNull(repository.getMetadata())
    }

    @Test
    fun `VaultMetadata toString no expone sal ni DEK envueltas`() {
        val metadata = VaultMetadata(
            saltBase64 = "c2FsdF9zZWNyZXRh",
            kdfIterations = 310_000,
            masterWrappedDek = "v1:ivSecreto:ctSecreto",
            biometricWrappedDek = "v1:ivBioSecreto:ctBioSecreto",
            createdAtEpochMillis = 10L,
            updatedAtEpochMillis = 20L
        )

        val rendered = metadata.toString()

        assertFalse(rendered.contains("c2FsdF9zZWNyZXRh"))
        assertFalse(rendered.contains("ivSecreto"))
        assertFalse(rendered.contains("ivBioSecreto"))
        assertTrue(rendered.contains("hasBiometricKey=true"))
        assertTrue(rendered.contains("310000"))
    }
}

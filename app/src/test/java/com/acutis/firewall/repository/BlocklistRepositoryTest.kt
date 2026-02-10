package com.acutis.firewall.repository

import android.content.Context
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.repository.BlocklistRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class BlocklistRepositoryTest {

    private lateinit var blockedSiteDao: BlockedSiteDao
    private lateinit var context: Context
    private lateinit var repository: BlocklistRepository

    @Before
    fun setup() {
        blockedSiteDao = mockk(relaxed = true)
        context = mockk(relaxed = true)
        repository = BlocklistRepository(context, blockedSiteDao)
    }

    @Test
    fun `addCustomSite normalizes domain correctly`() = runTest {
        // Given
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        coEvery { blockedSiteDao.insert(any()) } returns 1L

        // When
        val result = repository.addCustomSite("https://www.Example.COM/path")

        // Then
        assertThat(result).isTrue()
        coVerify {
            blockedSiteDao.getSiteByDomain("example.com")
        }
    }

    @Test
    fun `addCustomSite returns false for duplicate domain`() = runTest {
        // Given
        val existingSite = BlockedSite(
            id = 1,
            domain = "example.com",
            category = BlockCategory.CUSTOM,
            isEnabled = true,
            isCustom = true
        )
        coEvery { blockedSiteDao.getSiteByDomain("example.com") } returns existingSite

        // When
        val result = repository.addCustomSite("example.com")

        // Then
        assertThat(result).isFalse()
        coVerify(exactly = 0) { blockedSiteDao.insert(any()) }
    }

    @Test
    fun `addCustomSite inserts site with correct properties`() = runTest {
        // Given
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        val capturedSite = slot<BlockedSite>()
        coEvery { blockedSiteDao.insert(capture(capturedSite)) } returns 1L

        // When
        repository.addCustomSite("test.example.com")

        // Then
        assertThat(capturedSite.captured.domain).isEqualTo("test.example.com")
        assertThat(capturedSite.captured.category).isEqualTo(BlockCategory.CUSTOM)
        assertThat(capturedSite.captured.isEnabled).isTrue()
        assertThat(capturedSite.captured.isCustom).isTrue()
    }

    @Test
    fun `toggleSite flips enabled state`() = runTest {
        // Given
        val site = BlockedSite(
            id = 1,
            domain = "example.com",
            category = BlockCategory.ADULT,
            isEnabled = true
        )
        coEvery { blockedSiteDao.update(any()) } just Runs

        // When
        repository.toggleSite(site)

        // Then
        coVerify {
            blockedSiteDao.update(match { it.isEnabled == false && it.id == 1L })
        }
    }

    @Test
    fun `setCategoryEnabled updates all sites in category`() = runTest {
        // Given
        coEvery { blockedSiteDao.setCategoryEnabled(any(), any()) } just Runs

        // When
        repository.setCategoryEnabled(BlockCategory.ADULT, false)

        // Then
        coVerify {
            blockedSiteDao.setCategoryEnabled(BlockCategory.ADULT, false)
        }
    }

    @Test
    fun `isDomainBlocked normalizes domain before checking`() = runTest {
        // Given
        coEvery { blockedSiteDao.isDomainBlocked("example.com") } returns true

        // When
        val result = repository.isDomainBlocked("HTTPS://WWW.Example.COM/")

        // Then
        assertThat(result).isTrue()
        coVerify { blockedSiteDao.isDomainBlocked("example.com") }
    }

    @Test
    fun `normalizeDomain removes http prefix`() = runTest {
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        coEvery { blockedSiteDao.insert(any()) } returns 1L

        repository.addCustomSite("http://example.com")

        coVerify { blockedSiteDao.getSiteByDomain("example.com") }
    }

    @Test
    fun `normalizeDomain removes https prefix`() = runTest {
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        coEvery { blockedSiteDao.insert(any()) } returns 1L

        repository.addCustomSite("https://example.com")

        coVerify { blockedSiteDao.getSiteByDomain("example.com") }
    }

    @Test
    fun `normalizeDomain removes www prefix`() = runTest {
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        coEvery { blockedSiteDao.insert(any()) } returns 1L

        repository.addCustomSite("www.example.com")

        coVerify { blockedSiteDao.getSiteByDomain("example.com") }
    }

    @Test
    fun `normalizeDomain removes path`() = runTest {
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        coEvery { blockedSiteDao.insert(any()) } returns 1L

        repository.addCustomSite("example.com/path/to/page")

        coVerify { blockedSiteDao.getSiteByDomain("example.com") }
    }

    @Test
    fun `normalizeDomain converts to lowercase`() = runTest {
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        coEvery { blockedSiteDao.insert(any()) } returns 1L

        repository.addCustomSite("EXAMPLE.COM")

        coVerify { blockedSiteDao.getSiteByDomain("example.com") }
    }

    @Test
    fun `getEnabledSites returns flow from dao`() = runTest {
        // Given
        val sites = listOf(
            BlockedSite(1, "example.com", BlockCategory.ADULT, true),
            BlockedSite(2, "test.com", BlockCategory.MALWARE, true)
        )
        every { blockedSiteDao.getEnabledSites() } returns flowOf(sites)

        // When
        val flow = repository.getEnabledSites()

        // Then
        flow.collect { result ->
            assertThat(result).hasSize(2)
            assertThat(result[0].domain).isEqualTo("example.com")
        }
    }

    @Test
    fun `removeSite deletes site from dao`() = runTest {
        // Given
        val site = BlockedSite(1, "example.com", BlockCategory.CUSTOM, true)
        coEvery { blockedSiteDao.delete(any()) } just Runs

        // When
        repository.removeSite(site)

        // Then
        coVerify { blockedSiteDao.delete(site) }
    }

    @Test
    fun `removeSiteByDomain normalizes and deletes`() = runTest {
        // Given
        coEvery { blockedSiteDao.deleteByDomain(any()) } just Runs

        // When
        repository.removeSiteByDomain("https://www.Example.COM/path")

        // Then
        coVerify { blockedSiteDao.deleteByDomain("example.com") }
    }
}

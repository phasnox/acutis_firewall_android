package com.acutis.firewall.repository

import android.content.Context
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.CustomBlocklistDao
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.repository.CustomBlocklistRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class CustomBlocklistRepositoryTest {

    private lateinit var context: Context
    private lateinit var customBlocklistDao: CustomBlocklistDao
    private lateinit var blockedSiteDao: BlockedSiteDao
    private lateinit var repository: CustomBlocklistRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        customBlocklistDao = mockk(relaxed = true)
        blockedSiteDao = mockk(relaxed = true)
        repository = CustomBlocklistRepository(context, customBlocklistDao, blockedSiteDao)
    }

    @Test
    fun `createList creates list with correct properties`() = runTest {
        // Given
        val capturedList = slot<CustomBlocklist>()
        coEvery { customBlocklistDao.insert(capture(capturedList)) } returns 1L

        // When
        val id = repository.createList("Test List", "Test Description")

        // Then
        assertThat(id).isEqualTo(1L)
        assertThat(capturedList.captured.name).isEqualTo("Test List")
        assertThat(capturedList.captured.description).isEqualTo("Test Description")
        assertThat(capturedList.captured.isEnabled).isTrue()
    }

    @Test
    fun `createList uses empty description by default`() = runTest {
        // Given
        val capturedList = slot<CustomBlocklist>()
        coEvery { customBlocklistDao.insert(capture(capturedList)) } returns 1L

        // When
        repository.createList("Test List")

        // Then
        assertThat(capturedList.captured.description).isEmpty()
    }

    @Test
    fun `addDomainToList normalizes domain`() = runTest {
        // Given
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        coEvery { blockedSiteDao.insert(any()) } returns 1L

        // When
        val result = repository.addDomainToList(1L, "https://www.Example.COM/path")

        // Then
        assertThat(result).isTrue()
        coVerify {
            blockedSiteDao.getSiteByDomain("example.com")
        }
    }

    @Test
    fun `addDomainToList returns false for duplicate in same list`() = runTest {
        // Given
        val existingSite = BlockedSite(
            id = 1,
            domain = "example.com",
            category = BlockCategory.CUSTOM,
            isEnabled = true,
            isCustom = true,
            customListId = 1L
        )
        coEvery { blockedSiteDao.getSiteByDomain("example.com") } returns existingSite

        // When
        val result = repository.addDomainToList(1L, "example.com")

        // Then
        assertThat(result).isFalse()
        coVerify(exactly = 0) { blockedSiteDao.insert(any()) }
    }

    @Test
    fun `addDomainToList allows same domain in different list`() = runTest {
        // Given
        val existingSite = BlockedSite(
            id = 1,
            domain = "example.com",
            category = BlockCategory.CUSTOM,
            isEnabled = true,
            isCustom = true,
            customListId = 1L
        )
        coEvery { blockedSiteDao.getSiteByDomain("example.com") } returns existingSite
        coEvery { blockedSiteDao.insert(any()) } returns 2L

        // When
        val result = repository.addDomainToList(2L, "example.com")

        // Then
        assertThat(result).isTrue()
        coVerify { blockedSiteDao.insert(any()) }
    }

    @Test
    fun `addDomainToList inserts site with correct customListId`() = runTest {
        // Given
        coEvery { blockedSiteDao.getSiteByDomain(any()) } returns null
        val capturedSite = slot<BlockedSite>()
        coEvery { blockedSiteDao.insert(capture(capturedSite)) } returns 1L

        // When
        repository.addDomainToList(5L, "example.com")

        // Then
        assertThat(capturedSite.captured.customListId).isEqualTo(5L)
        assertThat(capturedSite.captured.domain).isEqualTo("example.com")
        assertThat(capturedSite.captured.category).isEqualTo(BlockCategory.CUSTOM)
        assertThat(capturedSite.captured.isCustom).isTrue()
        assertThat(capturedSite.captured.isEnabled).isTrue()
    }

    @Test
    fun `deleteList removes sites first then list`() = runTest {
        // Given
        coEvery { blockedSiteDao.deleteByListId(any()) } just Runs
        coEvery { customBlocklistDao.deleteById(any()) } just Runs

        // When
        repository.deleteList(1L)

        // Then
        coVerifyOrder {
            blockedSiteDao.deleteByListId(1L)
            customBlocklistDao.deleteById(1L)
        }
    }

    @Test
    fun `toggleListEnabled updates list and sites`() = runTest {
        // Given
        coEvery { customBlocklistDao.setEnabled(any(), any()) } just Runs
        coEvery { blockedSiteDao.setListSitesEnabled(any(), any()) } just Runs

        // When
        repository.toggleListEnabled(1L, false)

        // Then
        coVerify {
            customBlocklistDao.setEnabled(1L, false)
            blockedSiteDao.setListSitesEnabled(1L, false)
        }
    }

    @Test
    fun `toggleDomainEnabled flips enabled state`() = runTest {
        // Given
        val site = BlockedSite(
            id = 1,
            domain = "example.com",
            category = BlockCategory.CUSTOM,
            isEnabled = true,
            customListId = 1L
        )
        coEvery { blockedSiteDao.update(any()) } just Runs

        // When
        repository.toggleDomainEnabled(site)

        // Then
        coVerify {
            blockedSiteDao.update(match { it.isEnabled == false && it.id == 1L })
        }
    }

    @Test
    fun `removeDomainFromList deletes site`() = runTest {
        // Given
        val site = BlockedSite(
            id = 1,
            domain = "example.com",
            category = BlockCategory.CUSTOM,
            customListId = 1L
        )
        coEvery { blockedSiteDao.delete(any()) } just Runs

        // When
        repository.removeDomainFromList(site)

        // Then
        coVerify { blockedSiteDao.delete(site) }
    }

    @Test
    fun `getAllLists returns flow from dao`() = runTest {
        // Given
        val lists = listOf(
            CustomBlocklist(1, "List 1", "Desc 1", true),
            CustomBlocklist(2, "List 2", "Desc 2", true)
        )
        every { customBlocklistDao.getAllLists() } returns flowOf(lists)

        // When
        val flow = repository.getAllLists()

        // Then
        flow.collect { result ->
            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("List 1")
        }
    }

    @Test
    fun `getEnabledLists returns only enabled lists`() = runTest {
        // Given
        val lists = listOf(
            CustomBlocklist(1, "Enabled List", "", true)
        )
        every { customBlocklistDao.getEnabledLists() } returns flowOf(lists)

        // When
        val flow = repository.getEnabledLists()

        // Then
        flow.collect { result ->
            assertThat(result).hasSize(1)
            assertThat(result[0].isEnabled).isTrue()
        }
    }

    @Test
    fun `getSitesInList returns sites for specific list`() = runTest {
        // Given
        val sites = listOf(
            BlockedSite(1, "site1.com", BlockCategory.CUSTOM, true, true, 1L),
            BlockedSite(2, "site2.com", BlockCategory.CUSTOM, true, true, 1L)
        )
        every { blockedSiteDao.getSitesByListId(1L) } returns flowOf(sites)

        // When
        val flow = repository.getSitesInList(1L)

        // Then
        flow.collect { result ->
            assertThat(result).hasSize(2)
        }
    }

    @Test
    fun `updateList calls dao update`() = runTest {
        // Given
        val list = CustomBlocklist(1, "Updated Name", "Updated Desc", true)
        coEvery { customBlocklistDao.update(any()) } just Runs

        // When
        repository.updateList(list)

        // Then
        coVerify { customBlocklistDao.update(list) }
    }
}

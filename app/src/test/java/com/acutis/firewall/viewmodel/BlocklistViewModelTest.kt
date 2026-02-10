package com.acutis.firewall.viewmodel

import app.cash.turbine.test
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.BlocklistRepository
import com.acutis.firewall.data.repository.CustomBlocklistRepository
import com.acutis.firewall.ui.screens.blocklist.BlocklistViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
class BlocklistViewModelTest {

    private lateinit var blocklistRepository: BlocklistRepository
    private lateinit var customBlocklistRepository: CustomBlocklistRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var viewModel: BlocklistViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testSites = listOf(
        BlockedSite(1, "adult1.com", BlockCategory.ADULT, true),
        BlockedSite(2, "adult2.com", BlockCategory.ADULT, true),
        BlockedSite(3, "malware.com", BlockCategory.MALWARE, true),
        BlockedSite(4, "gambling.com", BlockCategory.GAMBLING, false),
        BlockedSite(5, "social.com", BlockCategory.SOCIAL_MEDIA, false),
        BlockedSite(6, "custom.com", BlockCategory.CUSTOM, true, true)
    )

    private val testCustomLists = listOf(
        CustomBlocklist(1, "Work", "Work distractions", true),
        CustomBlocklist(2, "Entertainment", "Entertainment sites", false)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        blocklistRepository = mockk(relaxed = true)
        customBlocklistRepository = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)

        every { blocklistRepository.getAllSites() } returns flowOf(testSites)
        every { blocklistRepository.getCustomSites() } returns flowOf(testSites.filter { it.isCustom })
        every { customBlocklistRepository.getAllLists() } returns flowOf(testCustomLists)
        every { settingsDataStore.adultBlockEnabled } returns flowOf(true)
        every { settingsDataStore.malwareBlockEnabled } returns flowOf(true)
        every { settingsDataStore.gamblingBlockEnabled } returns flowOf(false)
        every { settingsDataStore.socialMediaBlockEnabled } returns flowOf(false)
        every { settingsDataStore.pinEnabled } returns flowOf(false)
        every { settingsDataStore.hasPin() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BlocklistViewModel {
        return BlocklistViewModel(blocklistRepository, customBlocklistRepository, settingsDataStore)
    }

    @Test
    fun `initial state counts sites by category`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.adultCount).isEqualTo(2)
            assertThat(state.malwareCount).isEqualTo(1)
            assertThat(state.gamblingCount).isEqualTo(1)
            assertThat(state.socialMediaCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state reflects category enabled states`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.adultEnabled).isTrue()
            assertThat(state.malwareEnabled).isTrue()
            assertThat(state.gamblingEnabled).isFalse()
            assertThat(state.socialMediaEnabled).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state includes custom lists`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.customLists).hasSize(2)
            assertThat(state.customLists[0].name).isEqualTo("Work")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSelectedTab updates selected tab`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.setSelectedTab(1)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedTab).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showAddDialog sets flag to true`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showAddDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hideAddDialog sets flag to false`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.showAddDialog()

        // When
        viewModel.hideAddDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showAddListDialog sets flag to true`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showAddListDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddListDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hideAddListDialog sets flag to false`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.showAddListDialog()

        // When
        viewModel.hideAddListDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddListDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleAdultBlock without PIN directly executes`() = runTest {
        // Given
        coEvery { settingsDataStore.setAdultBlockEnabled(any()) } just Runs
        coEvery { blocklistRepository.setCategoryEnabled(any(), any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleAdultBlock(true)
        advanceUntilIdle()

        // Then
        coVerify { settingsDataStore.setAdultBlockEnabled(true) }
        coVerify { blocklistRepository.setCategoryEnabled(BlockCategory.ADULT, true) }
    }

    @Test
    fun `toggleAdultBlock with PIN shows dialog when disabling`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleAdultBlock(false) // disabling

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleAdultBlock with PIN does not show dialog when enabling`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        coEvery { settingsDataStore.setAdultBlockEnabled(any()) } just Runs
        coEvery { blocklistRepository.setCategoryEnabled(any(), any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleAdultBlock(true) // enabling
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { settingsDataStore.setAdultBlockEnabled(true) }
    }

    @Test
    fun `addCustomSite calls repository and hides dialog`() = runTest {
        // Given
        coEvery { blocklistRepository.addCustomSite(any()) } returns true
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.showAddDialog()

        // When
        viewModel.addCustomSite("example.com")
        advanceUntilIdle()

        // Then
        coVerify { blocklistRepository.addCustomSite("example.com") }
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createList calls repository and hides dialog`() = runTest {
        // Given
        coEvery { customBlocklistRepository.createList(any(), any()) } returns 1L
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.showAddListDialog()

        // When
        viewModel.createList("New List", "Description")
        advanceUntilIdle()

        // Then
        coVerify { customBlocklistRepository.createList("New List", "Description") }
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddListDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteList with PIN shows dialog`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()
        val list = CustomBlocklist(1, "Test", "", true)

        // When
        viewModel.deleteList(list)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verifyPin with correct pin executes pending action`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.verifyPin("1234") } returns true
        coEvery { settingsDataStore.setAdultBlockEnabled(any()) } just Runs
        coEvery { blocklistRepository.setCategoryEnabled(any(), any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleAdultBlock(false)

        // When
        viewModel.verifyPin("1234")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isFalse()
            assertThat(state.pinError).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { settingsDataStore.setAdultBlockEnabled(false) }
    }

    @Test
    fun `verifyPin with wrong pin shows error`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.verifyPin("0000") } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleAdultBlock(false)

        // When
        viewModel.verifyPin("0000")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.pinError).isTrue()
            assertThat(state.showPinDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissPinDialog hides dialog and clears error`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.toggleAdultBlock(false)

        // When
        viewModel.dismissPinDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isFalse()
            assertThat(state.pinError).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleListEnabled with PIN shows dialog when disabling`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()
        val list = CustomBlocklist(1, "Test", "", true) // enabled list

        // When
        viewModel.toggleListEnabled(list)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleListEnabled without PIN directly executes`() = runTest {
        // Given
        coEvery { customBlocklistRepository.toggleListEnabled(any(), any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()
        val list = CustomBlocklist(1, "Test", "", true)

        // When
        viewModel.toggleListEnabled(list)
        advanceUntilIdle()

        // Then
        coVerify { customBlocklistRepository.toggleListEnabled(1L, false) }
    }
}

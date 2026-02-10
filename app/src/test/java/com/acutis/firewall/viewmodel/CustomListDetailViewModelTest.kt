package com.acutis.firewall.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.CustomBlocklistRepository
import com.acutis.firewall.ui.screens.blocklist.CustomListDetailViewModel
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
class CustomListDetailViewModelTest {

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var customBlocklistRepository: CustomBlocklistRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var viewModel: CustomListDetailViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testList = CustomBlocklist(1, "Work Distractions", "Sites to block during work", true)

    private val testSites = listOf(
        BlockedSite(1, "facebook.com", BlockCategory.CUSTOM, true, true, 1L),
        BlockedSite(2, "twitter.com", BlockCategory.CUSTOM, true, true, 1L),
        BlockedSite(3, "reddit.com", BlockCategory.CUSTOM, false, true, 1L)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        savedStateHandle = mockk(relaxed = true)
        customBlocklistRepository = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)

        every { savedStateHandle.get<Long>("listId") } returns 1L
        every { customBlocklistRepository.getListById(1L) } returns flowOf(testList)
        every { customBlocklistRepository.getSitesInList(1L) } returns flowOf(testSites)
        every { settingsDataStore.pinEnabled } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CustomListDetailViewModel {
        return CustomListDetailViewModel(savedStateHandle, customBlocklistRepository, settingsDataStore)
    }

    @Test
    fun `initial state loads list and sites`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.list).isEqualTo(testList)
            assertThat(state.sites).hasSize(3)
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state reflects pin enabled state`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.pinEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showAddDomainDialog sets flag`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showAddDomainDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddDomainDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hideAddDomainDialog clears flag`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.showAddDomainDialog()

        // When
        viewModel.hideAddDomainDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddDomainDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addDomain calls repository and hides dialog`() = runTest {
        // Given
        coEvery { customBlocklistRepository.addDomainToList(any(), any()) } returns true
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.showAddDomainDialog()

        // When
        viewModel.addDomain("instagram.com")
        advanceUntilIdle()

        // Then
        coVerify { customBlocklistRepository.addDomainToList(1L, "instagram.com") }
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddDomainDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removeDomain without PIN directly removes`() = runTest {
        // Given
        coEvery { customBlocklistRepository.removeDomainFromList(any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.removeDomain(testSites[0])
        advanceUntilIdle()

        // Then
        coVerify { customBlocklistRepository.removeDomainFromList(testSites[0]) }
    }

    @Test
    fun `removeDomain with PIN shows dialog`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.removeDomain(testSites[0])

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDomainEnabled without PIN directly toggles`() = runTest {
        // Given
        coEvery { customBlocklistRepository.toggleDomainEnabled(any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        val disabledSite = testSites[2] // isEnabled = false

        // When - enabling does not require PIN
        viewModel.toggleDomainEnabled(disabledSite)
        advanceUntilIdle()

        // Then
        coVerify { customBlocklistRepository.toggleDomainEnabled(disabledSite) }
    }

    @Test
    fun `toggleDomainEnabled with PIN when disabling shows dialog`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        val enabledSite = testSites[0] // isEnabled = true

        // When - disabling requires PIN
        viewModel.toggleDomainEnabled(enabledSite)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDomainEnabled with PIN when enabling does not show dialog`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        coEvery { customBlocklistRepository.toggleDomainEnabled(any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        val disabledSite = testSites[2] // isEnabled = false

        // When - enabling does not require PIN
        viewModel.toggleDomainEnabled(disabledSite)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { customBlocklistRepository.toggleDomainEnabled(disabledSite) }
    }

    @Test
    fun `verifyPin with correct pin executes pending action`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.verifyPin("1234") } returns true
        coEvery { customBlocklistRepository.removeDomainFromList(any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.removeDomain(testSites[0])

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
        coVerify { customBlocklistRepository.removeDomainFromList(testSites[0]) }
    }

    @Test
    fun `verifyPin with wrong pin shows error`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.verifyPin("0000") } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.removeDomain(testSites[0])

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
    fun `dismissPinDialog clears state`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.removeDomain(testSites[0])

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
    fun `updateListName calls repository`() = runTest {
        // Given
        coEvery { customBlocklistRepository.updateList(any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateListName("New Name")
        advanceUntilIdle()

        // Then
        coVerify { customBlocklistRepository.updateList(match { it.name == "New Name" }) }
    }

    @Test
    fun `updateListDescription calls repository`() = runTest {
        // Given
        coEvery { customBlocklistRepository.updateList(any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateListDescription("New description")
        advanceUntilIdle()

        // Then
        coVerify { customBlocklistRepository.updateList(match { it.description == "New description" }) }
    }

    @Test
    fun `updateListName does nothing when no list loaded`() = runTest {
        // Given
        every { customBlocklistRepository.getListById(1L) } returns flowOf(null)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateListName("New Name")
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { customBlocklistRepository.updateList(any()) }
    }
}

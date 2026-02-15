package com.acutis.firewall.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.acutis.firewall.blocklist.BlocklistDownloader
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.BlocklistRepository
import com.acutis.firewall.ui.screens.home.HomeViewModel
import com.acutis.firewall.ui.screens.home.PendingAction
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
class HomeViewModelTest {

    private lateinit var application: Application
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var blocklistRepository: BlocklistRepository
    private lateinit var blocklistDownloader: BlocklistDownloader
    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        blocklistRepository = mockk(relaxed = true)
        blocklistDownloader = mockk(relaxed = true)

        every { settingsDataStore.firewallEnabled } returns flowOf(false)
        every { settingsDataStore.pinEnabled } returns flowOf(false)
        every { settingsDataStore.lockdownModeDetected } returns flowOf(false)
        every { blocklistRepository.getEnabledCount() } returns flowOf(100)
        every { settingsDataStore.hasPin() } returns false
        coEvery { settingsDataStore.setLockdownModeDetected(any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(application, settingsDataStore, blocklistRepository, blocklistDownloader)
    }

    @Test
    fun `initial state has correct default values`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isFirewallEnabled).isFalse()
            assertThat(state.isPinEnabled).isFalse()
            assertThat(state.blockedSitesCount).isEqualTo(100)
            assertThat(state.showPinDialog).isFalse()
            assertThat(state.pinError).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToggleFirewall without pin shows no dialog`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(false)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onToggleFirewall()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToggleFirewall with pin enabled shows pin dialog when disabling`() = runTest {
        // Given
        every { settingsDataStore.firewallEnabled } returns flowOf(true)
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onToggleFirewall()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isTrue()
            assertThat(state.pendingAction).isEqualTo(PendingAction.TOGGLE_FIREWALL)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToggleFirewall with pin enabled does not show dialog when enabling`() = runTest {
        // Given
        every { settingsDataStore.firewallEnabled } returns flowOf(false) // firewall is OFF
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onToggleFirewall()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPinEntered with correct pin hides dialog`() = runTest {
        // Given
        every { settingsDataStore.firewallEnabled } returns flowOf(true)
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        every { settingsDataStore.verifyPin("1234") } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleFirewall()
        advanceUntilIdle()

        // When
        viewModel.onPinEntered("1234")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isFalse()
            assertThat(state.pinError).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPinEntered with wrong pin shows error`() = runTest {
        // Given
        every { settingsDataStore.firewallEnabled } returns flowOf(true)
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        every { settingsDataStore.verifyPin("0000") } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleFirewall()
        advanceUntilIdle()

        // When
        viewModel.onPinEntered("0000")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.pinError).isTrue()
            assertThat(state.showPinDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPinDialogDismiss hides dialog and clears pending action`() = runTest {
        // Given
        every { settingsDataStore.firewallEnabled } returns flowOf(true)
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleFirewall()
        advanceUntilIdle()

        // When
        viewModel.onPinDialogDismiss()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinDialog).isFalse()
            assertThat(state.pendingAction).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blockedSitesCount updates from repository`() = runTest {
        // Given
        every { blocklistRepository.getEnabledCount() } returns flowOf(500)
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.blockedSitesCount).isEqualTo(500)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissUpdateResult clears update result`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.dismissUpdateResult()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showUpdateResult).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lockdown mode detected shows warning in UI state`() = runTest {
        // Given
        every { settingsDataStore.lockdownModeDetected } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showLockdownWarning).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lockdown mode not detected hides warning in UI state`() = runTest {
        // Given
        every { settingsDataStore.lockdownModeDetected } returns flowOf(false)
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showLockdownWarning).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissLockdownWarning calls setLockdownModeDetected with false`() = runTest {
        // Given
        every { settingsDataStore.lockdownModeDetected } returns flowOf(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.dismissLockdownWarning()
        advanceUntilIdle()

        // Then
        coVerify { settingsDataStore.setLockdownModeDetected(false) }
    }

    @Test
    fun `showVpnConflictAlert sets showVpnConflictAlert to true`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showVpnConflictAlert()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showVpnConflictAlert).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissVpnConflictAlert sets showVpnConflictAlert to false`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showVpnConflictAlert()
        advanceUntilIdle()

        // When
        viewModel.dismissVpnConflictAlert()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showVpnConflictAlert).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state has showLockdownWarning as false by default`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showLockdownWarning).isFalse()
            assertThat(state.showVpnConflictAlert).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

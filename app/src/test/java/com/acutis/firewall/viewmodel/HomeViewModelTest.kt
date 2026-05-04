package com.acutis.firewall.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.acutis.firewall.blocklist.BlocklistDownloader
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.TimeRule
import com.acutis.firewall.data.db.entities.TimeRuleAction
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.data.repository.BlocklistRepository
import com.acutis.firewall.data.repository.TimeRuleRepository
import com.acutis.firewall.ui.screens.home.HomeViewModel
import com.acutis.firewall.ui.screens.home.PendingAction
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var timeRuleRepository: TimeRuleRepository
    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        blocklistRepository = mockk(relaxed = true)
        blocklistDownloader = mockk(relaxed = true)
        timeRuleRepository = mockk(relaxed = true)

        every { settingsDataStore.firewallEnabled } returns flowOf(false)
        every { settingsDataStore.pinEnabled } returns flowOf(false)
        every { settingsDataStore.lockdownModeDetected } returns flowOf(false)
        every { settingsDataStore.adultBlockEnabled } returns flowOf(true)
        every { settingsDataStore.malwareBlockEnabled } returns flowOf(true)
        every { settingsDataStore.gamblingBlockEnabled } returns flowOf(false)
        every { settingsDataStore.socialMediaBlockEnabled } returns flowOf(false)
        every { blocklistRepository.getEnabledCount() } returns flowOf(100)
        every { settingsDataStore.hasPin() } returns false
        coEvery { settingsDataStore.setLockdownModeDetected(any()) } just Runs
        coEvery { settingsDataStore.areDefaultTimeRulesCreated() } returns true
        coEvery { settingsDataStore.setDefaultTimeRulesCreated(any()) } just Runs
        coEvery { settingsDataStore.isInitialDownloadPromptShown() } returns true
        coEvery { settingsDataStore.setInitialDownloadPromptShown(any()) } just Runs
        coEvery { timeRuleRepository.addRule(any()) } returns 1L
        every { timeRuleRepository.createDailyLimitRule(any(), any(), any(), any(), any(), any()) } returns TimeRule(
            domain = null,
            category = BlockCategory.SOCIAL_MEDIA,
            action = TimeRuleAction.ALLOW,
            dailyLimitMinutes = 30,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(application, settingsDataStore, blocklistRepository, blocklistDownloader, timeRuleRepository)
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
    fun `updateBlocklists shows failure dialog when every category fails`() = runTest {
        // Given - every category download fails (mimics the SSL-intercept scenario
        // where the user's network blocks the blocklist CDNs).
        val failed = BlocklistDownloader.DownloadResult(
            success = false,
            domainsAdded = 0,
            urlsAttempted = 2,
            urlsSucceeded = 0,
            error = "SSLHandshakeException: trust anchor missing"
        )
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(any(), any()) } returns failed
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateBlocklists()
        advanceUntilIdle()

        // Then - user must be informed the update failed (not "Update Complete").
        val state = viewModel.uiState.value
        assertThat(state.showUpdateResult).isTrue()
        assertThat(state.updateResultIsError).isTrue()
        assertThat(state.updateResultTitle).isEqualTo("Update Failed")
        assertThat(state.updateResultMessage).contains("SSLHandshakeException")
    }

    @Test
    fun `updateBlocklists shows incomplete dialog when some categories fail`() = runTest {
        // Given - adult succeeds, the rest fail.
        val ok = BlocklistDownloader.DownloadResult(
            success = true,
            domainsAdded = 50_000,
            urlsAttempted = 2,
            urlsSucceeded = 2
        )
        val failed = BlocklistDownloader.DownloadResult(
            success = false,
            domainsAdded = 0,
            urlsAttempted = 1,
            urlsSucceeded = 0,
            error = "IOException: timeout"
        )
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT, any()) } returns ok
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE, any()) } returns failed
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING, any()) } returns failed
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.SOCIAL_MEDIA, any()) } returns failed
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateBlocklists()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state.showUpdateResult).isTrue()
        assertThat(state.updateResultIsError).isTrue()
        assertThat(state.updateResultTitle).isEqualTo("Update Incomplete")
        assertThat(state.updateResultMessage).contains("Adult: 50000")
        assertThat(state.updateResultMessage).contains("Failed")
    }

    @Test
    fun `updateBlocklists shows complete dialog when all succeed`() = runTest {
        // Given
        val ok = BlocklistDownloader.DownloadResult(
            success = true,
            domainsAdded = 10_000,
            urlsAttempted = 1,
            urlsSucceeded = 1
        )
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(any(), any()) } returns ok
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateBlocklists()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state.showUpdateResult).isTrue()
        assertThat(state.updateResultIsError).isFalse()
        assertThat(state.updateResultTitle).isEqualTo("Update Complete")
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

    @Test
    fun `creates default time rules on first launch`() = runTest {
        // Given - default time rules not yet created
        coEvery { settingsDataStore.areDefaultTimeRulesCreated() } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        coVerify { timeRuleRepository.addRule(any()) }
        coVerify { settingsDataStore.setDefaultTimeRulesCreated(true) }
    }

    @Test
    fun `does not create default time rules if already created`() = runTest {
        // Given - default time rules already created
        coEvery { settingsDataStore.areDefaultTimeRulesCreated() } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { timeRuleRepository.addRule(any()) }
        coVerify(exactly = 0) { settingsDataStore.setDefaultTimeRulesCreated(any()) }
    }

    @Test
    fun `isTogglingFirewall starts as false`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isTogglingFirewall).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isTogglingFirewall is set to true when toggling firewall`() = runTest {
        // Given - firewall is off, no PIN required
        every { settingsDataStore.firewallEnabled } returns flowOf(false)
        every { settingsDataStore.pinEnabled } returns flowOf(false)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onToggleFirewall()

        // Then - should be toggling (before state actually changes)
        assertThat(viewModel.uiState.value.isTogglingFirewall).isTrue()
    }

    @Test
    fun `isTogglingFirewall is cleared when firewall state changes`() = runTest {
        // Given - use MutableStateFlow to simulate state changes
        val firewallEnabledFlow = MutableStateFlow(false)
        every { settingsDataStore.firewallEnabled } returns firewallEnabledFlow
        every { settingsDataStore.pinEnabled } returns flowOf(false)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When - toggle firewall
        viewModel.onToggleFirewall()
        assertThat(viewModel.uiState.value.isTogglingFirewall).isTrue()

        // Simulate firewall becoming enabled
        firewallEnabledFlow.value = true
        advanceUntilIdle()

        // Then - toggling should be cleared
        assertThat(viewModel.uiState.value.isTogglingFirewall).isFalse()
        assertThat(viewModel.uiState.value.isFirewallEnabled).isTrue()
    }

    @Test
    fun `isTogglingFirewall is set after PIN verification when disabling`() = runTest {
        // Given - firewall is on, PIN is required
        every { settingsDataStore.firewallEnabled } returns flowOf(true)
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        every { settingsDataStore.verifyPin("1234") } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        // When - toggle (should show PIN dialog first)
        viewModel.onToggleFirewall()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.showPinDialog).isTrue()
        assertThat(viewModel.uiState.value.isTogglingFirewall).isFalse()

        // Enter correct PIN
        viewModel.onPinEntered("1234")
        advanceUntilIdle()

        // Then - should now be toggling
        assertThat(viewModel.uiState.value.showPinDialog).isFalse()
        assertThat(viewModel.uiState.value.isTogglingFirewall).isTrue()
    }

    @Test
    fun `first launch with no domains shows download prompt`() = runTest {
        // Given - no domains downloaded yet AND prompt has never been shown
        every { blocklistRepository.getEnabledCount() } returns flowOf(0)
        coEvery { settingsDataStore.isInitialDownloadPromptShown() } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - prompt is visible
        assertThat(viewModel.uiState.value.showInitialDownloadPrompt).isTrue()
        // And no download was triggered without consent
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(any(), any()) }
    }

    @Test
    fun `first launch does not show prompt if already shown`() = runTest {
        // Given - no domains, but prompt was already shown previously
        every { blocklistRepository.getEnabledCount() } returns flowOf(0)
        coEvery { settingsDataStore.isInitialDownloadPromptShown() } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value.showInitialDownloadPrompt).isFalse()
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(any(), any()) }
    }

    @Test
    fun `first launch does not show prompt if domains exist`() = runTest {
        // Given - domains already exist (e.g., user re-installed and DB persisted)
        every { blocklistRepository.getEnabledCount() } returns flowOf(50)
        coEvery { settingsDataStore.isInitialDownloadPromptShown() } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value.showInitialDownloadPrompt).isFalse()
    }

    @Test
    fun `onAcceptInitialDownload hides prompt, marks shown, and downloads enabled categories`() = runTest {
        // Given - first launch state, only adult and malware enabled
        every { blocklistRepository.getEnabledCount() } returns flowOf(0)
        coEvery { settingsDataStore.isInitialDownloadPromptShown() } returns false
        val ok = BlocklistDownloader.DownloadResult(
            success = true,
            domainsAdded = 1000,
            urlsAttempted = 1,
            urlsSucceeded = 1
        )
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(any(), any()) } returns ok
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onAcceptInitialDownload()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value.showInitialDownloadPrompt).isFalse()
        coVerify { settingsDataStore.setInitialDownloadPromptShown(true) }
        // Adult + malware default to enabled, gambling + social do not
        coVerify { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT, any()) }
        coVerify { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE, any()) }
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING, any()) }
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.SOCIAL_MEDIA, any()) }
    }

    @Test
    fun `onDeclineInitialDownload hides prompt, marks shown, and does not download`() = runTest {
        // Given - first launch state
        every { blocklistRepository.getEnabledCount() } returns flowOf(0)
        coEvery { settingsDataStore.isInitialDownloadPromptShown() } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onDeclineInitialDownload()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value.showInitialDownloadPrompt).isFalse()
        coVerify { settingsDataStore.setInitialDownloadPromptShown(true) }
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(any(), any()) }
    }

    @Test
    fun `updateBlocklists skips disabled categories`() = runTest {
        // Given - only malware enabled
        every { settingsDataStore.adultBlockEnabled } returns flowOf(false)
        every { settingsDataStore.malwareBlockEnabled } returns flowOf(true)
        every { settingsDataStore.gamblingBlockEnabled } returns flowOf(false)
        every { settingsDataStore.socialMediaBlockEnabled } returns flowOf(false)
        val ok = BlocklistDownloader.DownloadResult(
            success = true,
            domainsAdded = 100,
            urlsAttempted = 1,
            urlsSucceeded = 1
        )
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(any(), any()) } returns ok
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateBlocklists()
        advanceUntilIdle()

        // Then - only malware was attempted
        coVerify(exactly = 1) { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE, any()) }
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT, any()) }
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING, any()) }
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.SOCIAL_MEDIA, any()) }
    }

    @Test
    fun `updateBlocklists with no enabled categories shows nothing-to-update message`() = runTest {
        // Given - all categories disabled
        every { settingsDataStore.adultBlockEnabled } returns flowOf(false)
        every { settingsDataStore.malwareBlockEnabled } returns flowOf(false)
        every { settingsDataStore.gamblingBlockEnabled } returns flowOf(false)
        every { settingsDataStore.socialMediaBlockEnabled } returns flowOf(false)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateBlocklists()
        advanceUntilIdle()

        // Then - no network calls, message indicates nothing to do
        coVerify(exactly = 0) { blocklistDownloader.downloadAndSaveBlocklist(any(), any()) }
        val state = viewModel.uiState.value
        assertThat(state.showUpdateResult).isTrue()
        assertThat(state.updateResultTitle).isEqualTo("Nothing to Update")
    }

    @Test
    fun `isTogglingFirewall is cleared when disabling completes`() = runTest {
        // Given - use MutableStateFlow to simulate state changes
        val firewallEnabledFlow = MutableStateFlow(true)
        every { settingsDataStore.firewallEnabled } returns firewallEnabledFlow
        every { settingsDataStore.pinEnabled } returns flowOf(false)
        viewModel = createViewModel()
        advanceUntilIdle()

        // Verify initial state
        assertThat(viewModel.uiState.value.isFirewallEnabled).isTrue()

        // When - toggle firewall to disable
        viewModel.onToggleFirewall()
        assertThat(viewModel.uiState.value.isTogglingFirewall).isTrue()

        // Simulate firewall becoming disabled
        firewallEnabledFlow.value = false
        advanceUntilIdle()

        // Then - toggling should be cleared
        assertThat(viewModel.uiState.value.isTogglingFirewall).isFalse()
        assertThat(viewModel.uiState.value.isFirewallEnabled).isFalse()
    }
}

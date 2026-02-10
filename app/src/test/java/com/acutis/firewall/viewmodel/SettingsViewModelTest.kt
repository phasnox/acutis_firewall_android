package com.acutis.firewall.viewmodel

import app.cash.turbine.test
import com.acutis.firewall.blocklist.BlocklistDownloader
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.preferences.SettingsDataStore
import com.acutis.firewall.ui.screens.settings.SettingsPendingAction
import com.acutis.firewall.ui.screens.settings.SettingsViewModel
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
class SettingsViewModelTest {

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var blocklistDownloader: BlocklistDownloader
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        settingsDataStore = mockk(relaxed = true)
        blocklistDownloader = mockk(relaxed = true)

        every { settingsDataStore.pinEnabled } returns flowOf(false)
        every { settingsDataStore.autoStartEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(settingsDataStore, blocklistDownloader)
    }

    @Test
    fun `initial state has correct default values`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isPinEnabled).isFalse()
            assertThat(state.hasPin).isFalse()
            assertThat(state.autoStartEnabled).isTrue()
            assertThat(state.showPinSetupDialog).isFalse()
            assertThat(state.showPinVerifyDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPinToggle enables shows setup dialog when no pin exists`() = runTest {
        // Given
        every { settingsDataStore.hasPin() } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onPinToggle(true)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinSetupDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPinToggle enables pin directly when pin already exists`() = runTest {
        // Given
        every { settingsDataStore.hasPin() } returns true
        coEvery { settingsDataStore.setPinEnabled(true) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onPinToggle(true)
        advanceUntilIdle()

        // Then
        coVerify { settingsDataStore.setPinEnabled(true) }
    }

    @Test
    fun `onPinToggle disable shows verify dialog`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onPinToggle(false)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinVerifyDialog).isTrue()
            assertThat(state.pendingAction).isEqualTo(SettingsPendingAction.DISABLE_PIN)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onChangePin shows verify dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onChangePin()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinVerifyDialog).isTrue()
            assertThat(state.pendingAction).isEqualTo(SettingsPendingAction.CHANGE_PIN)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPinSetup saves pin and enables`() = runTest {
        // Given
        coEvery { settingsDataStore.setPinEnabled(true) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onPinSetup("1234")
        advanceUntilIdle()

        // Then
        verify { settingsDataStore.setPin("1234") }
        coVerify { settingsDataStore.setPinEnabled(true) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinSetupDialog).isFalse()
            assertThat(state.hasPin).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPinVerified with correct pin disables pin`() = runTest {
        // Given
        every { settingsDataStore.pinEnabled } returns flowOf(true)
        every { settingsDataStore.hasPin() } returns true
        every { settingsDataStore.verifyPin("1234") } returns true
        coEvery { settingsDataStore.setPinEnabled(false) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPinToggle(false)

        // When
        viewModel.onPinVerified("1234")
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinVerifyDialog).isFalse()
            assertThat(state.pinError).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { settingsDataStore.setPinEnabled(false) }
    }

    @Test
    fun `onPinVerified with correct pin for change shows setup dialog`() = runTest {
        // Given
        every { settingsDataStore.hasPin() } returns true
        every { settingsDataStore.verifyPin("1234") } returns true
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onChangePin()

        // When
        viewModel.onPinVerified("1234")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinVerifyDialog).isFalse()
            assertThat(state.showPinSetupDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPinVerified with wrong pin shows error`() = runTest {
        // Given
        every { settingsDataStore.hasPin() } returns true
        every { settingsDataStore.verifyPin("0000") } returns false
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onChangePin()

        // When
        viewModel.onPinVerified("0000")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.pinError).isTrue()
            assertThat(state.showPinVerifyDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissPinSetupDialog hides dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onPinToggle(true)

        // When
        viewModel.dismissPinSetupDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinSetupDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissPinVerifyDialog hides dialog and clears pending`() = runTest {
        // Given
        every { settingsDataStore.hasPin() } returns true
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onChangePin()

        // When
        viewModel.dismissPinVerifyDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showPinVerifyDialog).isFalse()
            assertThat(state.pinError).isFalse()
            assertThat(state.pendingAction).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAutoStartToggle calls datastore`() = runTest {
        // Given
        coEvery { settingsDataStore.setAutoStartEnabled(any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onAutoStartToggle(false)
        advanceUntilIdle()

        // Then
        coVerify { settingsDataStore.setAutoStartEnabled(false) }
    }

    @Test
    fun `downloadBlocklists updates state during download`() = runTest {
        // Given
        val adultResult = BlocklistDownloader.DownloadResult(true, 1000)
        val malwareResult = BlocklistDownloader.DownloadResult(true, 500)
        val gamblingResult = BlocklistDownloader.DownloadResult(true, 200)
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT) } returns adultResult
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE) } returns malwareResult
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING) } returns gamblingResult
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.downloadBlocklists()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isDownloading).isFalse()
            assertThat(state.lastDownloadResult).contains("1700")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `downloadBlocklists downloads all categories`() = runTest {
        // Given
        val adultResult = BlocklistDownloader.DownloadResult(true, 100)
        val malwareResult = BlocklistDownloader.DownloadResult(true, 50)
        val gamblingResult = BlocklistDownloader.DownloadResult(true, 25)
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT) } returns adultResult
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE) } returns malwareResult
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING) } returns gamblingResult
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.downloadBlocklists()
        advanceUntilIdle()

        // Then - all three categories should be downloaded
        coVerify { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.ADULT) }
        coVerify { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.MALWARE) }
        coVerify { blocklistDownloader.downloadAndSaveBlocklist(BlockCategory.GAMBLING) }
    }

    @Test
    fun `clearDownloadResult clears result`() = runTest {
        // Given
        val adultResult = BlocklistDownloader.DownloadResult(true, 100)
        coEvery { blocklistDownloader.downloadAndSaveBlocklist(any()) } returns adultResult
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.downloadBlocklists()
        advanceUntilIdle()

        // When
        viewModel.clearDownloadResult()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.lastDownloadResult).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

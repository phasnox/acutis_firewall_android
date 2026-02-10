package com.acutis.firewall.viewmodel

import app.cash.turbine.test
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.CustomBlocklist
import com.acutis.firewall.data.db.entities.TimeRule
import com.acutis.firewall.data.db.entities.TimeRuleAction
import com.acutis.firewall.data.repository.CustomBlocklistRepository
import com.acutis.firewall.data.repository.TimeRuleRepository
import com.acutis.firewall.ui.screens.timerules.*
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
class TimeRulesViewModelTest {

    private lateinit var timeRuleRepository: TimeRuleRepository
    private lateinit var customBlocklistRepository: CustomBlocklistRepository
    private lateinit var viewModel: TimeRulesViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testRules = listOf(
        TimeRule(1, "youtube.com", null, null, TimeRuleAction.ALLOW, 60, null, null, null, null),
        TimeRule(2, null, BlockCategory.SOCIAL_MEDIA, null, TimeRuleAction.BLOCK, null, 22, 0, 6, 0)
    )

    private val testCustomLists = listOf(
        CustomBlocklist(1, "Work", "Work distractions", true),
        CustomBlocklist(2, "Gaming", "Gaming sites", true)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        timeRuleRepository = mockk(relaxed = true)
        customBlocklistRepository = mockk(relaxed = true)

        every { timeRuleRepository.getAllRules() } returns flowOf(testRules)
        every { customBlocklistRepository.getAllLists() } returns flowOf(testCustomLists)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TimeRulesViewModel {
        return TimeRulesViewModel(timeRuleRepository, customBlocklistRepository)
    }

    @Test
    fun `initial state loads rules from repository`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.rules).hasSize(2)
            assertThat(state.rules[0].domain).isEqualTo("youtube.com")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state loads custom lists from repository`() = runTest {
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
    fun `showAddDialog resets new rule data and shows dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showAddDialog()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddDialog).isTrue()
            assertThat(state.editingRule).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.newRuleData.test {
            val data = awaitItem()
            assertThat(data.domain).isEmpty()
            assertThat(data.targetType).isEqualTo(TargetType.DOMAIN)
            assertThat(data.action).isEqualTo(TimeRuleAction.BLOCK)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showEditDialog populates data from domain rule`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        val rule = TimeRule(
            id = 1,
            domain = "youtube.com",
            category = null,
            customListId = null,
            action = TimeRuleAction.ALLOW,
            dailyLimitMinutes = 60,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            daysOfWeek = "1,2,3,4,5"
        )

        // When
        viewModel.showEditDialog(rule)

        // Then
        viewModel.newRuleData.test {
            val data = awaitItem()
            assertThat(data.targetType).isEqualTo(TargetType.DOMAIN)
            assertThat(data.domain).isEqualTo("youtube.com")
            assertThat(data.action).isEqualTo(TimeRuleAction.ALLOW)
            assertThat(data.ruleType).isEqualTo(RuleType.DAILY_LIMIT)
            assertThat(data.dailyLimitMinutes).isEqualTo(60)
            assertThat(data.selectedDays).containsExactly(1, 2, 3, 4, 5)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showEditDialog populates data from category rule`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        val rule = TimeRule(
            id = 2,
            domain = null,
            category = BlockCategory.SOCIAL_MEDIA,
            customListId = null,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = null,
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            daysOfWeek = "1,2,3,4,5,6,7"
        )

        // When
        viewModel.showEditDialog(rule)

        // Then
        viewModel.newRuleData.test {
            val data = awaitItem()
            assertThat(data.targetType).isEqualTo(TargetType.PRESET_CATEGORY)
            assertThat(data.category).isEqualTo(BlockCategory.SOCIAL_MEDIA)
            assertThat(data.ruleType).isEqualTo(RuleType.SCHEDULE)
            assertThat(data.startHour).isEqualTo(22)
            assertThat(data.endHour).isEqualTo(6)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showEditDialog populates data from custom list rule`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        val rule = TimeRule(
            id = 3,
            domain = null,
            category = null,
            customListId = 5L,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = null,
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0
        )

        // When
        viewModel.showEditDialog(rule)

        // Then
        viewModel.newRuleData.test {
            val data = awaitItem()
            assertThat(data.targetType).isEqualTo(TargetType.CUSTOM_LIST)
            assertThat(data.customListId).isEqualTo(5L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hideAddDialog hides dialog and clears editing rule`() = runTest {
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
            assertThat(state.editingRule).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateNewRuleData updates the data`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        val newData = NewRuleData(
            targetType = TargetType.PRESET_CATEGORY,
            domain = "",
            category = BlockCategory.GAMBLING,
            action = TimeRuleAction.BLOCK,
            ruleType = RuleType.SCHEDULE,
            startHour = 18,
            startMinute = 0,
            endHour = 23,
            endMinute = 30,
            selectedDays = listOf(6, 7)
        )

        // When
        viewModel.updateNewRuleData(newData)

        // Then
        viewModel.newRuleData.test {
            val data = awaitItem()
            assertThat(data.targetType).isEqualTo(TargetType.PRESET_CATEGORY)
            assertThat(data.category).isEqualTo(BlockCategory.GAMBLING)
            assertThat(data.startHour).isEqualTo(18)
            assertThat(data.endMinute).isEqualTo(30)
            assertThat(data.selectedDays).containsExactly(6, 7)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveRule with domain target creates rule with domain`() = runTest {
        // Given
        val capturedRule = slot<TimeRule>()
        coEvery { timeRuleRepository.addRule(capture(capturedRule)) } returns 1L
        every { timeRuleRepository.createScheduleRule(any(), any(), any(), any(), any(), any(), any(), any(), any()) } answers {
            TimeRule(
                domain = firstArg(),
                category = secondArg(),
                customListId = thirdArg(),
                action = arg(3),
                dailyLimitMinutes = null,
                startHour = arg(4),
                startMinute = arg(5),
                endHour = arg(6),
                endMinute = arg(7),
                daysOfWeek = (arg(8) as List<Int>).joinToString(",")
            )
        }
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNewRuleData(NewRuleData(
            targetType = TargetType.DOMAIN,
            domain = "youtube.com",
            action = TimeRuleAction.BLOCK,
            ruleType = RuleType.SCHEDULE,
            startHour = 8,
            startMinute = 0,
            endHour = 17,
            endMinute = 0
        ))

        // When
        viewModel.saveRule()
        advanceUntilIdle()

        // Then
        coVerify { timeRuleRepository.addRule(any()) }
        assertThat(capturedRule.captured.domain).isEqualTo("youtube.com")
        assertThat(capturedRule.captured.category).isNull()
        assertThat(capturedRule.captured.customListId).isNull()
    }

    @Test
    fun `saveRule with category target creates rule with category`() = runTest {
        // Given
        val capturedRule = slot<TimeRule>()
        coEvery { timeRuleRepository.addRule(capture(capturedRule)) } returns 1L
        every { timeRuleRepository.createScheduleRule(any(), any(), any(), any(), any(), any(), any(), any(), any()) } answers {
            TimeRule(
                domain = firstArg(),
                category = secondArg(),
                customListId = thirdArg(),
                action = arg(3),
                dailyLimitMinutes = null,
                startHour = arg(4),
                startMinute = arg(5),
                endHour = arg(6),
                endMinute = arg(7),
                daysOfWeek = (arg(8) as List<Int>).joinToString(",")
            )
        }
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNewRuleData(NewRuleData(
            targetType = TargetType.PRESET_CATEGORY,
            domain = "ignored",
            category = BlockCategory.ADULT,
            action = TimeRuleAction.BLOCK,
            ruleType = RuleType.SCHEDULE,
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59
        ))

        // When
        viewModel.saveRule()
        advanceUntilIdle()

        // Then
        coVerify { timeRuleRepository.addRule(any()) }
        assertThat(capturedRule.captured.domain).isNull()
        assertThat(capturedRule.captured.category).isEqualTo(BlockCategory.ADULT)
    }

    @Test
    fun `saveRule with custom list target creates rule with customListId`() = runTest {
        // Given
        val capturedRule = slot<TimeRule>()
        coEvery { timeRuleRepository.addRule(capture(capturedRule)) } returns 1L
        every { timeRuleRepository.createDailyLimitRule(any(), any(), any(), any(), any(), any()) } answers {
            TimeRule(
                domain = firstArg(),
                category = secondArg(),
                customListId = thirdArg(),
                action = arg(3),
                dailyLimitMinutes = arg(4),
                startHour = null,
                startMinute = null,
                endHour = null,
                endMinute = null,
                daysOfWeek = (arg(5) as List<Int>).joinToString(",")
            )
        }
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNewRuleData(NewRuleData(
            targetType = TargetType.CUSTOM_LIST,
            customListId = 5L,
            action = TimeRuleAction.ALLOW,
            ruleType = RuleType.DAILY_LIMIT,
            dailyLimitMinutes = 120
        ))

        // When
        viewModel.saveRule()
        advanceUntilIdle()

        // Then
        coVerify { timeRuleRepository.addRule(any()) }
        assertThat(capturedRule.captured.domain).isNull()
        assertThat(capturedRule.captured.category).isNull()
        assertThat(capturedRule.captured.customListId).isEqualTo(5L)
    }

    @Test
    fun `saveRule updates existing rule when editing`() = runTest {
        // Given
        val existingRule = TimeRule(
            id = 1,
            domain = "youtube.com",
            category = null,
            customListId = null,
            action = TimeRuleAction.ALLOW,
            dailyLimitMinutes = 60,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null
        )
        coEvery { timeRuleRepository.updateRule(any()) } just Runs
        every { timeRuleRepository.createDailyLimitRule(any(), any(), any(), any(), any(), any()) } answers {
            TimeRule(
                domain = firstArg(),
                category = secondArg(),
                customListId = thirdArg(),
                action = arg(3),
                dailyLimitMinutes = arg(4),
                startHour = null,
                startMinute = null,
                endHour = null,
                endMinute = null,
                daysOfWeek = (arg(5) as List<Int>).joinToString(",")
            )
        }
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showEditDialog(existingRule)

        // When
        viewModel.saveRule()
        advanceUntilIdle()

        // Then
        coVerify { timeRuleRepository.updateRule(match { it.id == 1L }) }
        coVerify(exactly = 0) { timeRuleRepository.addRule(any()) }
    }

    @Test
    fun `deleteRule calls repository delete`() = runTest {
        // Given
        coEvery { timeRuleRepository.deleteRule(any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()
        val rule = testRules[0]

        // When
        viewModel.deleteRule(rule)
        advanceUntilIdle()

        // Then
        coVerify { timeRuleRepository.deleteRule(rule) }
    }

    @Test
    fun `toggleRule calls repository setRuleEnabled`() = runTest {
        // Given
        coEvery { timeRuleRepository.setRuleEnabled(any(), any()) } just Runs
        viewModel = createViewModel()
        advanceUntilIdle()
        val rule = TimeRule(
            id = 1,
            domain = "test.com",
            category = null,
            customListId = null,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = 60,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            isEnabled = true
        )

        // When
        viewModel.toggleRule(rule)
        advanceUntilIdle()

        // Then
        coVerify { timeRuleRepository.setRuleEnabled(1L, false) }
    }

    @Test
    fun `saveRule hides dialog after saving`() = runTest {
        // Given
        coEvery { timeRuleRepository.addRule(any()) } returns 1L
        every { timeRuleRepository.createScheduleRule(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk()
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.showAddDialog()
        viewModel.updateNewRuleData(NewRuleData(domain = "test.com"))

        // When
        viewModel.saveRule()
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.showAddDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

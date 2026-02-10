package com.acutis.firewall.repository

import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.TimeRuleDao
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.acutis.firewall.data.db.entities.TimeRule
import com.acutis.firewall.data.db.entities.TimeRuleAction
import com.acutis.firewall.data.repository.TimeRuleRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class TimeRuleRepositoryTest {

    private lateinit var timeRuleDao: TimeRuleDao
    private lateinit var blockedSiteDao: BlockedSiteDao
    private lateinit var repository: TimeRuleRepository

    @Before
    fun setup() {
        timeRuleDao = mockk(relaxed = true)
        blockedSiteDao = mockk(relaxed = true)
        repository = TimeRuleRepository(timeRuleDao, blockedSiteDao)
    }

    @Test
    fun `createScheduleRule creates rule with correct properties`() {
        // When
        val rule = repository.createScheduleRule(
            domain = "example.com",
            category = null,
            customListId = null,
            action = TimeRuleAction.BLOCK,
            startHour = 8,
            startMinute = 30,
            endHour = 17,
            endMinute = 0,
            daysOfWeek = listOf(1, 2, 3, 4, 5)
        )

        // Then
        assertThat(rule.domain).isEqualTo("example.com")
        assertThat(rule.category).isNull()
        assertThat(rule.customListId).isNull()
        assertThat(rule.action).isEqualTo(TimeRuleAction.BLOCK)
        assertThat(rule.startHour).isEqualTo(8)
        assertThat(rule.startMinute).isEqualTo(30)
        assertThat(rule.endHour).isEqualTo(17)
        assertThat(rule.endMinute).isEqualTo(0)
        assertThat(rule.dailyLimitMinutes).isNull()
        assertThat(rule.daysOfWeek).isEqualTo("1,2,3,4,5")
    }

    @Test
    fun `createDailyLimitRule creates rule with correct properties`() {
        // When
        val rule = repository.createDailyLimitRule(
            domain = "youtube.com",
            category = null,
            customListId = null,
            action = TimeRuleAction.ALLOW,
            limitMinutes = 60,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7)
        )

        // Then
        assertThat(rule.domain).isEqualTo("youtube.com")
        assertThat(rule.action).isEqualTo(TimeRuleAction.ALLOW)
        assertThat(rule.dailyLimitMinutes).isEqualTo(60)
        assertThat(rule.startHour).isNull()
        assertThat(rule.endHour).isNull()
        assertThat(rule.daysOfWeek).isEqualTo("1,2,3,4,5,6,7")
    }

    @Test
    fun `createScheduleRule with category sets category correctly`() {
        // When
        val rule = repository.createScheduleRule(
            domain = null,
            category = BlockCategory.SOCIAL_MEDIA,
            customListId = null,
            action = TimeRuleAction.BLOCK,
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0
        )

        // Then
        assertThat(rule.domain).isNull()
        assertThat(rule.category).isEqualTo(BlockCategory.SOCIAL_MEDIA)
    }

    @Test
    fun `createScheduleRule with customListId sets customListId correctly`() {
        // When
        val rule = repository.createScheduleRule(
            domain = null,
            category = null,
            customListId = 5L,
            action = TimeRuleAction.BLOCK,
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0
        )

        // Then
        assertThat(rule.domain).isNull()
        assertThat(rule.category).isNull()
        assertThat(rule.customListId).isEqualTo(5L)
    }

    @Test
    fun `createDailyLimitRule with customListId sets customListId correctly`() {
        // When
        val rule = repository.createDailyLimitRule(
            domain = null,
            category = null,
            customListId = 3L,
            action = TimeRuleAction.ALLOW,
            limitMinutes = 120
        )

        // Then
        assertThat(rule.customListId).isEqualTo(3L)
        assertThat(rule.dailyLimitMinutes).isEqualTo(120)
    }

    @Test
    fun `addRule calls dao insert`() = runTest {
        // Given
        val rule = TimeRule(
            domain = "example.com",
            category = null,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = 60,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null
        )
        coEvery { timeRuleDao.insert(any()) } returns 1L

        // When
        val id = repository.addRule(rule)

        // Then
        assertThat(id).isEqualTo(1L)
        coVerify { timeRuleDao.insert(rule) }
    }

    @Test
    fun `updateRule calls dao update`() = runTest {
        // Given
        val rule = TimeRule(
            id = 1,
            domain = "example.com",
            category = null,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = 60,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null
        )
        coEvery { timeRuleDao.update(any()) } just Runs

        // When
        repository.updateRule(rule)

        // Then
        coVerify { timeRuleDao.update(rule) }
    }

    @Test
    fun `deleteRule calls dao delete`() = runTest {
        // Given
        val rule = TimeRule(
            id = 1,
            domain = "example.com",
            category = null,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = null,
            startHour = 8,
            startMinute = 0,
            endHour = 17,
            endMinute = 0
        )
        coEvery { timeRuleDao.delete(any()) } just Runs

        // When
        repository.deleteRule(rule)

        // Then
        coVerify { timeRuleDao.delete(rule) }
    }

    @Test
    fun `setRuleEnabled calls dao setEnabled`() = runTest {
        // Given
        coEvery { timeRuleDao.setEnabled(any(), any()) } just Runs

        // When
        repository.setRuleEnabled(1L, false)

        // Then
        coVerify { timeRuleDao.setEnabled(1L, false) }
    }

    @Test
    fun `getAllRules returns flow from dao`() = runTest {
        // Given
        val rules = listOf(
            TimeRule(1, "example.com", null, null, TimeRuleAction.BLOCK, 60, null, null, null, null),
            TimeRule(2, "test.com", null, null, TimeRuleAction.ALLOW, null, 8, 0, 17, 0)
        )
        every { timeRuleDao.getAllRules() } returns flowOf(rules)

        // When
        val flow = repository.getAllRules()

        // Then
        flow.collect { result ->
            assertThat(result).hasSize(2)
        }
    }

    @Test
    fun `getEnabledRules returns flow of enabled rules`() = runTest {
        // Given
        val rules = listOf(
            TimeRule(1, "example.com", null, null, TimeRuleAction.BLOCK, 60, null, null, null, null, isEnabled = true)
        )
        every { timeRuleDao.getEnabledRules() } returns flowOf(rules)

        // When
        val flow = repository.getEnabledRules()

        // Then
        flow.collect { result ->
            assertThat(result).hasSize(1)
            assertThat(result[0].isEnabled).isTrue()
        }
    }

    @Test
    fun `getRuleById returns rule from dao`() = runTest {
        // Given
        val rule = TimeRule(1, "example.com", null, null, TimeRuleAction.BLOCK, 60, null, null, null, null)
        coEvery { timeRuleDao.getRuleById(1L) } returns rule

        // When
        val result = repository.getRuleById(1L)

        // Then
        assertThat(result).isEqualTo(rule)
    }

    @Test
    fun `getRuleById returns null for non-existent rule`() = runTest {
        // Given
        coEvery { timeRuleDao.getRuleById(999L) } returns null

        // When
        val result = repository.getRuleById(999L)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `checkAndUpdateTimeRule returns false when no matching rules`() = runTest {
        // Given
        coEvery { timeRuleDao.getEnabledRulesList() } returns emptyList()

        // When
        val result = repository.checkAndUpdateTimeRule("example.com")

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `checkAndUpdateTimeRule matches direct domain`() = runTest {
        // Given
        val rule = TimeRule(
            id = 1,
            domain = "example.com",
            category = null,
            customListId = null,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = null,
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            daysOfWeek = "1,2,3,4,5,6,7",
            isEnabled = true
        )
        coEvery { timeRuleDao.getEnabledRulesList() } returns listOf(rule)

        // When
        val result = repository.checkAndUpdateTimeRule("example.com")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `checkAndUpdateTimeRule matches subdomain of rule domain`() = runTest {
        // Given
        val rule = TimeRule(
            id = 1,
            domain = "example.com",
            category = null,
            customListId = null,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = null,
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            daysOfWeek = "1,2,3,4,5,6,7",
            isEnabled = true
        )
        coEvery { timeRuleDao.getEnabledRulesList() } returns listOf(rule)

        // When
        val result = repository.checkAndUpdateTimeRule("www.example.com")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `checkAndUpdateTimeRule matches domain in category`() = runTest {
        // Given
        val rule = TimeRule(
            id = 1,
            domain = null,
            category = BlockCategory.ADULT,
            customListId = null,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = null,
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            daysOfWeek = "1,2,3,4,5,6,7",
            isEnabled = true
        )
        val site = BlockedSite(1, "adult-site.com", BlockCategory.ADULT, true)
        coEvery { timeRuleDao.getEnabledRulesList() } returns listOf(rule)
        coEvery { blockedSiteDao.getSiteByDomain("adult-site.com") } returns site

        // When
        val result = repository.checkAndUpdateTimeRule("adult-site.com")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `checkAndUpdateTimeRule matches domain in custom list`() = runTest {
        // Given
        val rule = TimeRule(
            id = 1,
            domain = null,
            category = null,
            customListId = 5L,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = null,
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            daysOfWeek = "1,2,3,4,5,6,7",
            isEnabled = true
        )
        val site = BlockedSite(1, "custom-site.com", BlockCategory.CUSTOM, true, true, 5L)
        coEvery { timeRuleDao.getEnabledRulesList() } returns listOf(rule)
        coEvery { blockedSiteDao.getSiteByDomain("custom-site.com") } returns site

        // When
        val result = repository.checkAndUpdateTimeRule("custom-site.com")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `checkAndUpdateTimeRule does not match domain in different custom list`() = runTest {
        // Given
        val rule = TimeRule(
            id = 1,
            domain = null,
            category = null,
            customListId = 5L,
            action = TimeRuleAction.BLOCK,
            dailyLimitMinutes = null,
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            daysOfWeek = "1,2,3,4,5,6,7",
            isEnabled = true
        )
        val site = BlockedSite(1, "other-site.com", BlockCategory.CUSTOM, true, true, 10L)
        coEvery { timeRuleDao.getEnabledRulesList() } returns listOf(rule)
        coEvery { blockedSiteDao.getSiteByDomain("other-site.com") } returns site

        // When
        val result = repository.checkAndUpdateTimeRule("other-site.com")

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `default daysOfWeek includes all days`() {
        // When
        val rule = repository.createScheduleRule(
            domain = "example.com",
            category = null,
            action = TimeRuleAction.BLOCK,
            startHour = 8,
            startMinute = 0,
            endHour = 17,
            endMinute = 0
        )

        // Then
        assertThat(rule.daysOfWeek).isEqualTo("1,2,3,4,5,6,7")
    }
}

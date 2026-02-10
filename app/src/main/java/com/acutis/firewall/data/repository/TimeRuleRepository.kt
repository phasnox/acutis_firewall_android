package com.acutis.firewall.data.repository

import android.util.Log
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.TimeRuleDao
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.TimeRule
import com.acutis.firewall.data.db.entities.TimeRuleAction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeRuleRepository @Inject constructor(
    private val timeRuleDao: TimeRuleDao,
    private val blockedSiteDao: BlockedSiteDao
) {
    companion object {
        private const val TAG = "TimeRuleRepository"
        // If no DNS query for 5 minutes, consider the session ended
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
    }
    fun getAllRules(): Flow<List<TimeRule>> = timeRuleDao.getAllRules()

    fun getEnabledRules(): Flow<List<TimeRule>> = timeRuleDao.getEnabledRules()

    suspend fun getRuleById(id: Long): TimeRule? = timeRuleDao.getRuleById(id)

    suspend fun addRule(rule: TimeRule): Long = timeRuleDao.insert(rule)

    suspend fun updateRule(rule: TimeRule) = timeRuleDao.update(rule)

    suspend fun deleteRule(rule: TimeRule) = timeRuleDao.delete(rule)

    suspend fun deleteRuleById(id: Long) = timeRuleDao.deleteById(id)

    suspend fun setRuleEnabled(id: Long, enabled: Boolean) = timeRuleDao.setEnabled(id, enabled)

    suspend fun updateUsedMinutes(id: Long, minutes: Int) = timeRuleDao.updateUsedMinutes(id, minutes)

    suspend fun resetDailyUsage() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        timeRuleDao.resetDailyUsage(today)
    }

    /**
     * Check if a domain should be blocked based on time rules.
     * Also tracks DNS query timestamps and updates usage time.
     *
     * @param domain The domain being queried
     * @return true if the domain should be blocked
     */
    suspend fun checkAndUpdateTimeRule(domain: String): Boolean {
        // Get all enabled rules and filter to find matching ones
        val allRules = timeRuleDao.getEnabledRulesList()
        val matchingRules = allRules.filter { rule -> doesRuleMatchDomain(rule, domain) }

        if (matchingRules.isEmpty()) return false

        val now = LocalTime.now()
        val currentTimeMs = System.currentTimeMillis()
        val dayOfWeek = LocalDate.now().dayOfWeek.value.toString()

        for (rule in matchingRules) {
            if (!rule.daysOfWeek.split(",").contains(dayOfWeek)) continue

            // Check schedule-based rules first
            if (rule.startHour != null && rule.endHour != null) {
                val startTime = LocalTime.of(rule.startHour, rule.startMinute ?: 0)
                val endTime = LocalTime.of(rule.endHour, rule.endMinute ?: 0)

                val isInSchedule = if (startTime <= endTime) {
                    now in startTime..endTime
                } else {
                    now >= startTime || now <= endTime
                }

                if (isInSchedule && rule.action == TimeRuleAction.BLOCK) {
                    Log.d(TAG, "Domain $domain blocked by schedule rule (in blocked time window)")
                    return true
                }
                if (!isInSchedule && rule.action == TimeRuleAction.ALLOW) {
                    Log.d(TAG, "Domain $domain blocked by schedule rule (outside allowed time window)")
                    return true
                }
            }

            // Check daily limit rules with time tracking
            if (rule.dailyLimitMinutes != null) {
                val lastQuery = rule.lastQueryTimestamp
                var newUsedMinutes = rule.usedMinutesToday

                if (lastQuery != null) {
                    val timeSinceLastQuery = currentTimeMs - lastQuery

                    // If within session timeout, add elapsed time to usage
                    if (timeSinceLastQuery <= SESSION_TIMEOUT_MS) {
                        val additionalMinutes = (timeSinceLastQuery / 60000).toInt()
                        newUsedMinutes += additionalMinutes
                        Log.d(TAG, "Domain $domain: session active, adding $additionalMinutes min (total: $newUsedMinutes/${rule.dailyLimitMinutes})")
                    } else {
                        Log.d(TAG, "Domain $domain: session expired, starting new session")
                    }
                } else {
                    Log.d(TAG, "Domain $domain: first query of the day, starting session")
                }

                // Update the timestamp and usage
                timeRuleDao.updateUsageAndTimestamp(rule.id, newUsedMinutes, currentTimeMs)

                // Check if limit exceeded
                if (newUsedMinutes >= rule.dailyLimitMinutes) {
                    Log.d(TAG, "Domain $domain blocked: daily limit exceeded ($newUsedMinutes/${rule.dailyLimitMinutes} min)")
                    // For ALLOW rules, block when limit is exceeded
                    // For BLOCK rules, this doesn't apply (they're blocked by schedule)
                    if (rule.action == TimeRuleAction.ALLOW) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Check if a time rule matches the given domain.
     * A rule matches if:
     * - It has a specific domain that matches (with parent domain support)
     * - It has a category and the domain belongs to that category
     * - It has a customListId and the domain is in that custom list
     */
    private suspend fun doesRuleMatchDomain(rule: TimeRule, domain: String): Boolean {
        val normalizedDomain = domain.lowercase().trimEnd('.')

        // Check direct domain match
        if (rule.domain != null) {
            val ruleDomain = rule.domain.lowercase().trimEnd('.')
            if (normalizedDomain == ruleDomain) return true
            // Check if domain is a subdomain of the rule domain
            if (normalizedDomain.endsWith(".$ruleDomain")) return true
        }

        // Check category match - domain must be in blocked_sites with that category
        if (rule.category != null) {
            val site = blockedSiteDao.getSiteByDomain(normalizedDomain)
            if (site != null && site.category == rule.category) return true
            // Also check parent domains
            val parts = normalizedDomain.split(".")
            for (i in 1 until parts.size) {
                val parentDomain = parts.subList(i, parts.size).joinToString(".")
                val parentSite = blockedSiteDao.getSiteByDomain(parentDomain)
                if (parentSite != null && parentSite.category == rule.category) return true
            }
        }

        // Check custom list match
        if (rule.customListId != null) {
            val site = blockedSiteDao.getSiteByDomain(normalizedDomain)
            if (site != null && site.customListId == rule.customListId) return true
            // Also check parent domains
            val parts = normalizedDomain.split(".")
            for (i in 1 until parts.size) {
                val parentDomain = parts.subList(i, parts.size).joinToString(".")
                val parentSite = blockedSiteDao.getSiteByDomain(parentDomain)
                if (parentSite != null && parentSite.customListId == rule.customListId) return true
            }
        }

        return false
    }

    /**
     * Legacy method for checking without updating - use checkAndUpdateTimeRule instead
     */
    suspend fun shouldBlockDomain(domain: String): Boolean {
        return checkAndUpdateTimeRule(domain)
    }

    fun createDailyLimitRule(
        domain: String?,
        category: BlockCategory?,
        customListId: Long? = null,
        action: TimeRuleAction,
        limitMinutes: Int,
        daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
    ): TimeRule {
        return TimeRule(
            domain = domain,
            category = category,
            customListId = customListId,
            action = action,
            dailyLimitMinutes = limitMinutes,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            daysOfWeek = daysOfWeek.joinToString(",")
        )
    }

    fun createScheduleRule(
        domain: String?,
        category: BlockCategory?,
        customListId: Long? = null,
        action: TimeRuleAction,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
    ): TimeRule {
        return TimeRule(
            domain = domain,
            category = category,
            customListId = customListId,
            action = action,
            dailyLimitMinutes = null,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            daysOfWeek = daysOfWeek.joinToString(",")
        )
    }
}

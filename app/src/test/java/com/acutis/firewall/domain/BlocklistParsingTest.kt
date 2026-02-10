package com.acutis.firewall.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for blocklist file parsing logic.
 * This mirrors the logic in BlocklistDownloader.parseDomainFromLine()
 */
class BlocklistParsingTest {

    /**
     * Mirrors BlocklistDownloader.parseDomainFromLine() for testing
     */
    private fun parseDomainFromLine(line: String): String? {
        val trimmed = line.trim()

        // Skip comments and empty lines
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
            return null
        }

        // Handle hosts file format: "0.0.0.0 domain.com" or "127.0.0.1 domain.com"
        if (trimmed.startsWith("0.0.0.0") || trimmed.startsWith("127.0.0.1")) {
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size >= 2) {
                val domain = parts[1].lowercase().trim()
                if (isValidDomain(domain)) {
                    return domain
                }
            }
            return null
        }

        // Handle plain domain format
        val domain = trimmed.split(Regex("\\s+")).firstOrNull()?.lowercase()?.trim()
        if (domain != null && isValidDomain(domain)) {
            return domain
        }

        return null
    }

    /**
     * Mirrors BlocklistDownloader.isValidDomain() for testing
     */
    private fun isValidDomain(domain: String): Boolean {
        if (domain.isEmpty()) return false
        if (domain == "localhost") return false
        if (domain == "local") return false
        if (domain.startsWith("0.0.0.0")) return false
        if (domain.startsWith("127.")) return false
        if (domain.startsWith("#")) return false
        if (!domain.contains(".")) return false
        // Basic domain validation
        return domain.matches(Regex("^[a-z0-9][a-z0-9.-]*[a-z0-9]$"))
    }

    // Tests for parseDomainFromLine

    @Test
    fun `parses hosts file format with 0_0_0_0`() {
        assertThat(parseDomainFromLine("0.0.0.0 example.com")).isEqualTo("example.com")
        assertThat(parseDomainFromLine("0.0.0.0 malware.net")).isEqualTo("malware.net")
    }

    @Test
    fun `parses hosts file format with 127_0_0_1`() {
        assertThat(parseDomainFromLine("127.0.0.1 example.com")).isEqualTo("example.com")
        assertThat(parseDomainFromLine("127.0.0.1 blocked.site")).isEqualTo("blocked.site")
    }

    @Test
    fun `parses plain domain format`() {
        assertThat(parseDomainFromLine("example.com")).isEqualTo("example.com")
        assertThat(parseDomainFromLine("subdomain.example.com")).isEqualTo("subdomain.example.com")
    }

    @Test
    fun `skips comment lines starting with hash`() {
        assertThat(parseDomainFromLine("# This is a comment")).isNull()
        assertThat(parseDomainFromLine("#example.com")).isNull()
        assertThat(parseDomainFromLine("  # comment with spaces")).isNull()
    }

    @Test
    fun `skips comment lines starting with exclamation`() {
        assertThat(parseDomainFromLine("! AdBlock style comment")).isNull()
        assertThat(parseDomainFromLine("!example.com")).isNull()
    }

    @Test
    fun `skips empty lines`() {
        assertThat(parseDomainFromLine("")).isNull()
        assertThat(parseDomainFromLine("   ")).isNull()
        assertThat(parseDomainFromLine("\t")).isNull()
    }

    @Test
    fun `normalizes domain to lowercase`() {
        assertThat(parseDomainFromLine("0.0.0.0 EXAMPLE.COM")).isEqualTo("example.com")
        assertThat(parseDomainFromLine("Example.Com")).isEqualTo("example.com")
        assertThat(parseDomainFromLine("127.0.0.1 MiXeD.CaSe.CoM")).isEqualTo("mixed.case.com")
    }

    @Test
    fun `handles extra whitespace`() {
        assertThat(parseDomainFromLine("  0.0.0.0   example.com  ")).isEqualTo("example.com")
        assertThat(parseDomainFromLine("0.0.0.0\texample.com")).isEqualTo("example.com")
        assertThat(parseDomainFromLine("0.0.0.0    example.com   # comment")).isEqualTo("example.com")
    }

    @Test
    fun `handles hosts file with inline comments`() {
        // Some hosts files have comments after the domain
        assertThat(parseDomainFromLine("0.0.0.0 example.com # blocked")).isEqualTo("example.com")
    }

    // Tests for isValidDomain

    @Test
    fun `valid domains return true`() {
        assertThat(isValidDomain("example.com")).isTrue()
        assertThat(isValidDomain("sub.example.com")).isTrue()
        assertThat(isValidDomain("deep.sub.example.com")).isTrue()
        assertThat(isValidDomain("example123.com")).isTrue()
        assertThat(isValidDomain("my-site.com")).isTrue()
    }

    @Test
    fun `localhost is invalid`() {
        assertThat(isValidDomain("localhost")).isFalse()
    }

    @Test
    fun `local is invalid`() {
        assertThat(isValidDomain("local")).isFalse()
    }

    @Test
    fun `IP addresses are invalid`() {
        assertThat(isValidDomain("0.0.0.0")).isFalse()
        assertThat(isValidDomain("127.0.0.1")).isFalse()
        assertThat(isValidDomain("127.0.0.2")).isFalse()
    }

    @Test
    fun `domains without dot are invalid`() {
        assertThat(isValidDomain("nodot")).isFalse()
        assertThat(isValidDomain("singlelabel")).isFalse()
    }

    @Test
    fun `empty domain is invalid`() {
        assertThat(isValidDomain("")).isFalse()
    }

    @Test
    fun `domains starting with hash are invalid`() {
        assertThat(isValidDomain("#example.com")).isFalse()
    }

    @Test
    fun `domains with special characters are invalid`() {
        assertThat(isValidDomain("example!.com")).isFalse()
        assertThat(isValidDomain("exam ple.com")).isFalse()
        assertThat(isValidDomain("example@.com")).isFalse()
    }

    @Test
    fun `real-world hosts file entries`() {
        // Real entries from Steven Black hosts file
        assertThat(parseDomainFromLine("0.0.0.0 pornhub.com")).isEqualTo("pornhub.com")
        assertThat(parseDomainFromLine("0.0.0.0 www.pornhub.com")).isEqualTo("www.pornhub.com")
        assertThat(parseDomainFromLine("127.0.0.1 adserver.example.com")).isEqualTo("adserver.example.com")
        assertThat(parseDomainFromLine("# Title: Hosts contributed by Steven Black")).isNull()
        assertThat(parseDomainFromLine("")).isNull()
    }

    @Test
    fun `mixed content blocklist file`() {
        val lines = """
            # This is a blocklist file
            # Source: example.org

            0.0.0.0 malware-site.com
            0.0.0.0 tracking.example.net
            127.0.0.1 ads.company.com

            # End of section

            phishing-site.org
            fake-bank.com
        """.trimIndent().lines()

        val parsed = lines.mapNotNull { parseDomainFromLine(it) }

        assertThat(parsed).containsExactly(
            "malware-site.com",
            "tracking.example.net",
            "ads.company.com",
            "phishing-site.org",
            "fake-bank.com"
        )
    }
}

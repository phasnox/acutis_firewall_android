package com.acutis.firewall.blocklist

import android.content.Context
import com.acutis.firewall.data.db.BlockedSiteDao
import com.acutis.firewall.data.db.entities.BlockCategory
import com.acutis.firewall.data.db.entities.BlockedSite
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import javax.net.ssl.SSLHandshakeException

/**
 * Regression tests for the download-failure behavior flagged by the v1.0.3
 * manual test report (mitmproxy intercepting HTTPS). Each source throws a
 * real exception via the [HttpFetcher] seam so we exercise the downloader's
 * own failure aggregation without touching the network.
 */
class BlocklistDownloaderTest {

    private val context: Context = mockk(relaxed = true)
    private val dao: BlockedSiteDao = mockk(relaxed = true)

    private fun downloader(fetcher: HttpFetcher) = BlocklistDownloader(context, dao, fetcher)

    private fun linesFetcher(bodies: Map<String, List<String>>) = object : HttpFetcher {
        override fun <T> fetchLines(urlString: String, block: (Sequence<String>) -> T): T {
            val lines = bodies[urlString]
                ?: throw IllegalStateException("No canned response for $urlString")
            return block(lines.asSequence())
        }
    }

    private fun throwingFetcher(exception: () -> Exception) = object : HttpFetcher {
        override fun <T> fetchLines(urlString: String, block: (Sequence<String>) -> T): T {
            throw exception()
        }
    }

    @Test
    fun `happy path saves parsed domains and reports success`() = runTest {
        coEvery { dao.deleteByCategory(BlockCategory.ADULT) } just Runs
        coEvery { dao.insertAll(any()) } just Runs
        val url = "https://example.test/hosts"
        val fetcher = linesFetcher(
            mapOf(
                url to listOf(
                    "# comment",
                    "0.0.0.0 adult-one.example",
                    "0.0.0.0 adult-two.example",
                    "0.0.0.0 adult-one.example" // duplicate
                )
            )
        )

        val result = downloader(fetcher)
            .downloadAndSaveBlocklist(BlockCategory.ADULT, urls = listOf(url))

        assertThat(result.success).isTrue()
        assertThat(result.domainsAdded).isEqualTo(2)
        assertThat(result.urlsSucceeded).isEqualTo(1)
        assertThat(result.urlsAttempted).isEqualTo(1)
        coVerify { dao.deleteByCategory(BlockCategory.ADULT) }
        coVerify {
            dao.insertAll(withArg<List<BlockedSite>> { sites ->
                assertThat(sites.map { it.domain })
                    .containsExactly("adult-one.example", "adult-two.example")
            })
        }
    }

    @Test
    fun `all sources failing leaves existing data untouched`() = runTest {
        val urlA = "https://a.test/hosts"
        val urlB = "https://b.test/hosts"
        val fetcher = throwingFetcher {
            SSLHandshakeException("trust anchor for certification path not found")
        }

        val result = downloader(fetcher)
            .downloadAndSaveBlocklist(BlockCategory.MALWARE, urls = listOf(urlA, urlB))

        assertThat(result.success).isFalse()
        assertThat(result.domainsAdded).isEqualTo(0)
        assertThat(result.urlsAttempted).isEqualTo(2)
        assertThat(result.urlsSucceeded).isEqualTo(0)
        assertThat(result.error).contains("SSLHandshakeException")
        // Critical: do not wipe the existing category when every source failed.
        coVerify(exactly = 0) { dao.deleteByCategory(any()) }
        coVerify(exactly = 0) { dao.insertAll(any()) }
    }

    @Test
    fun `non-200 response is treated as failure`() = runTest {
        val url = "https://broken.test/hosts"
        val fetcher = throwingFetcher { IOException("HTTP 500 from $url") }

        val result = downloader(fetcher)
            .downloadAndSaveBlocklist(BlockCategory.GAMBLING, urls = listOf(url))

        assertThat(result.success).isFalse()
        assertThat(result.error).contains("HTTP 500")
        coVerify(exactly = 0) { dao.deleteByCategory(any()) }
    }

    @Test
    fun `partial failure still saves what succeeded`() = runTest {
        coEvery { dao.deleteByCategory(BlockCategory.SOCIAL_MEDIA) } just Runs
        coEvery { dao.insertAll(any()) } just Runs
        val goodUrl = "https://good.test/hosts"
        val badUrl = "https://bad.test/hosts"
        val fetcher = object : HttpFetcher {
            override fun <T> fetchLines(urlString: String, block: (Sequence<String>) -> T): T {
                return if (urlString == goodUrl) {
                    block(sequenceOf("0.0.0.0 social-one.example"))
                } else {
                    throw IOException("boom")
                }
            }
        }

        val result = downloader(fetcher)
            .downloadAndSaveBlocklist(BlockCategory.SOCIAL_MEDIA, urls = listOf(goodUrl, badUrl))

        assertThat(result.success).isTrue()
        assertThat(result.domainsAdded).isEqualTo(1)
        assertThat(result.urlsSucceeded).isEqualTo(1)
        assertThat(result.urlsAttempted).isEqualTo(2)
    }
}

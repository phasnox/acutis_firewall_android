package com.acutis.firewall.blocklist

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

interface HttpFetcher {
    @Throws(IOException::class)
    fun <T> fetchLines(urlString: String, block: (Sequence<String>) -> T): T
}

class DefaultHttpFetcher @Inject constructor() : HttpFetcher {
    override fun <T> fetchLines(urlString: String, block: (Sequence<String>) -> T): T {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.requestMethod = "GET"
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP $code from $urlString")
            }
            return connection.inputStream.bufferedReader().useLines(block)
        } finally {
            connection.disconnect()
        }
    }
}

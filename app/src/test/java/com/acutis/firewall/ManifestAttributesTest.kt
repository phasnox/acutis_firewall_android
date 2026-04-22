package com.acutis.firewall

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Locks in manifest attributes that the v1.0.3 manual test report flagged.
 * Predictive back (OnBackInvokedCallback) requires the application-level
 * attribute to be set; if the attribute is dropped, this test fails before
 * the regression ships.
 */
class ManifestAttributesTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private fun manifest(): File {
        // Tests run with working dir = app module root.
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("AndroidManifest.xml not found. cwd=${File(".").absolutePath}")
    }

    @Test
    fun `application enables OnBackInvokedCallback`() {
        val doc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest())

        val app = doc.getElementsByTagName("application").item(0)
            ?: error("No <application> element in AndroidManifest.xml")
        val attr = app.attributes.getNamedItemNS(androidNs, "enableOnBackInvokedCallback")
            ?: error("android:enableOnBackInvokedCallback attribute is missing")

        assertThat(attr.nodeValue).isEqualTo("true")
    }
}

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

    /**
     * Uninstall protection is silently load-bearing on these three attributes: drop
     * the permission and any app can spoof the admin broadcasts; drop the meta-data
     * or the exported flag and the component stops resolving as a device admin at
     * all, so protection just quietly stops working.
     */
    @Test
    fun `device admin receiver is declared correctly`() {
        val doc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest())

        val receivers = doc.getElementsByTagName("receiver")
        var admin: org.w3c.dom.Node? = null
        for (i in 0 until receivers.length) {
            val node = receivers.item(i)
            val name = node.attributes.getNamedItemNS(androidNs, "name")?.nodeValue
            if (name == ".admin.UninstallProtectionAdminReceiver") {
                admin = node
                break
            }
        }
        val receiver = admin ?: error("No .admin.UninstallProtectionAdminReceiver in AndroidManifest.xml")

        assertThat(receiver.attributes.getNamedItemNS(androidNs, "permission")?.nodeValue)
            .isEqualTo("android.permission.BIND_DEVICE_ADMIN")
        assertThat(receiver.attributes.getNamedItemNS(androidNs, "exported")?.nodeValue)
            .isEqualTo("true")

        val children = receiver.childNodes
        var hasDeviceAdminMetaData = false
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeName == "meta-data" &&
                child.attributes.getNamedItemNS(androidNs, "name")?.nodeValue ==
                "android.app.device_admin"
            ) {
                hasDeviceAdminMetaData = true
            }
        }
        assertThat(hasDeviceAdminMetaData).isTrue()
    }
}

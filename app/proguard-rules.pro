# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.acutis.firewall.data.db.entities.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep VPN service
-keep class com.acutis.firewall.service.FirewallVpnService { *; }

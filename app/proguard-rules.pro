# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.acutis.firewall.data.db.entities.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep VPN service
-keep class com.acutis.firewall.service.FirewallVpnService { *; }

# Google Tink / Crypto library - missing annotations at runtime (compile-time only)
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn com.google.errorprone.annotations.concurrent.LazyInit
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy

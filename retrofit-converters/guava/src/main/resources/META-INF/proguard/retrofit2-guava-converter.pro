# Keep generic signature of Optional (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking class com.google.common.base.Optional

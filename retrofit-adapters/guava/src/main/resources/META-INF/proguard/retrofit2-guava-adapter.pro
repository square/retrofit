# Keep generic signature of ListenableFuture (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking class com.google.common.util.concurrent.ListenableFuture

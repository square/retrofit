# Keep generic signature of RxJava (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking class rx.Single
-keep,allowobfuscation,allowshrinking class rx.Observable

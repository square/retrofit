# ADAPTER field is looked up reflectively.
-keepclassmembers class * extends com.squareup.wire.Message {
  public static com.squareup.wire.ProtoAdapter ADAPTER;
}

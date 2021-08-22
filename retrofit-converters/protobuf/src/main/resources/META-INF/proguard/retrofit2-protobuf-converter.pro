# Parser looked up reflectively.
-keepclassmembers class * implements com.google.protobuf.MessageLite {
  public static com.google.protobuf.Parser parser();

  # Fallback for v2.x.
  public static com.google.protobuf.Parser PARSER;
}

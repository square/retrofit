Google Nano Protocol Buffer Converter
=====================================

A `Converter` which uses [Nano Protocol Buffer][1] binary serialization.

To build this module, you will need to [install][2] the Android external\_protobuf module.

To use the converter, depend on the `nano` classifier:

```xml
<dependency>
  <groupId>com.squareup.retrofit</groupId>
  <artifactId>retrofit-converters</artifactId>
  <classifier>nano</classifier>
</dependency>
```

 [1]: https://github.com/android/platform_external_protobuf/tree/master/java/src/main/java/com/google/protobuf/nano
 [2]: https://github.com/android/platform_external_protobuf/blob/master/java/README.txt

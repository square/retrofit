include(
  ":retrofit",
  ":retrofit:android-test",
  ":retrofit:robovm-test",
  ":retrofit:test-helpers",

  ":retrofit-mock",

  ":retrofit-adapters:guava",
  ":retrofit-adapters:java8",
  ":retrofit-adapters:rxjava",
  ":retrofit-adapters:rxjava2",
  ":retrofit-adapters:rxjava3",
  ":retrofit-adapters:scala",

  ":retrofit-converters:gson",
  ":retrofit-converters:guava",
  ":retrofit-converters:jackson",
  ":retrofit-converters:java8",
  ":retrofit-converters:jaxb",
  ":retrofit-converters:jaxb3",
  ":retrofit-converters:moshi",
  ":retrofit-converters:protobuf",
  ":retrofit-converters:scalars",
  ":retrofit-converters:simplexml",
  ":retrofit-converters:wire",

  ":samples",
)

enableFeaturePreview("VERSION_CATALOGS")

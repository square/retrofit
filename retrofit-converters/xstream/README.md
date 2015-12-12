XStream Converter
====================

A `Converter` which uses [XStream][1] for XML serialization.

A default `XStream` instance will be created or one can be configured and passed to the
`XStreamConverter` construction to further control the serialization.


Android
-------

XStream depends on artifacts which are already provided by the Android platform. When specifying as
a Maven or Gradle dependency, exclude the following transitive dependencies: `stax:stax-api`,
`stax:stax`, and `xpp3:xpp3`.



 [1]: http://x-stream.github.io/

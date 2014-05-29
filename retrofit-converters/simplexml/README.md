Simple XML Converter
====================

A `Converter` which uses [Simple][1] for XML serialization.

A default `Serializer` instance will be created or one can be configured and passed to the
`SimpleXMLConverter` construction to further control the serialization.


Android
-------

Simple depends on artifacts which are already provided by the Android platform. When specifying as
a Maven or Gradle dependency, exclude the following transitive dependencies: `stax:stax-api`,
`stax:stax`, and `xpp3:xpp3`.



 [1]: http://simple.sourceforge.net/

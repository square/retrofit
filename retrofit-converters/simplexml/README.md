Simple XML Converter
====================

Deprecated – Please switch to the JAXB Converter
------------------------------------------------

The Simple XML project is no longer maintained. We recommend switching to the
[JAXB converter](https://github.com/square/retrofit/tree/master/retrofit-converters/jaxb).

-----

A `Converter` which uses [Simple][1] for XML serialization.

A default `Serializer` instance will be created or one can be configured and passed to the
`SimpleXMLConverter` construction to further control the serialization.


Android
-------

Simple depends on artifacts which are already provided by the Android platform. When specifying as
a Maven or Gradle dependency, exclude the following transitive dependencies: `stax:stax-api`,
`stax:stax`, and `xpp3:xpp3`.


Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>converter-simplexml</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:converter-simplexml:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [1]: http://simple.sourceforge.net/
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=converter-simplexml&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22converter-simplexml%22
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/

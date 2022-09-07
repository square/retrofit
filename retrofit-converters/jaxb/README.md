JAXB Converter
==============

A `Converter` which uses [JAXB][1] for serialization to and from XML.

A default `JAXBContext` instance will be created or one can be configured and passed
to `JaxbConverterFactory.create()` to further control the serialization.

**Note that JAXB does not work on Android.**

Download
--------

Download [the latest JAR][2] or grab via [Maven][3]:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>converter-jaxb</artifactId>
  <version>latest.version</version>
</dependency>
```
or [Gradle][3]:
```groovy
implementation 'com.squareup.retrofit2:converter-jaxb:latest.version'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



 [1]: https://github.com/eclipse-ee4j/jaxb-ri
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=converter-jaxb&v=LATEST
 [3]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.squareup.retrofit2%22%20a%3A%22converter-jaxb%22
 [snap]: https://s01.oss.sonatype.org/content/repositories/snapshots/

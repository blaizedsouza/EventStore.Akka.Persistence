### Event Store Plugin for Akka Persistence [![Build Status](https://travis-ci.org/EventStore/EventStore.Akka.Persistence.svg?branch=master)](https://travis-ci.org/EventStore/EventStore.Akka.Persistence) [![Version](https://img.shields.io/maven-central/v/com.geteventstore/akka-persistence-eventstore_2.11.svg?label=version)](http://search.maven.org/#search%7Cga%7C1%7Cg%3Acom.geteventstore%20AND%20akka-persistence-eventstore)

[Akka Persistence](http://doc.akka.io/docs/akka/2.5.1/scala/persistence.html) journal and snapshot-store backed by [Event Store](http://geteventstore.com/).

<table border="0">
  <tr>
    <td><a href="http://www.scala-lang.org">Scala</a> </td>
    <td>2.12.3/2.11.11</td>
  </tr>
  <tr>
    <td><a href="http://akka.io">Akka</a> </td>
    <td>2.5.6</td>
  </tr>
  <tr>
    <td><a href="https://github.com/EventStore/EventStore.JVM">EventStore client</a> </td>
    <td>5.0.0</td>
  </tr>
</table>

To use this plugin prior default one, add the following to `application.conf`:

```
akka.persistence {
  journal.plugin = eventstore.persistence.journal
  snapshot-store.plugin = eventstore.persistence.snapshot-store
}
```

To configure EventStore.JVM client, see it's [reference.conf](https://github.com/EventStore/EventStore.JVM/blob/master/src/main/resources/reference.conf)

### JSON serialization

Akka serializes your messages into binary data by default.
However you can [add your own serializer](http://doc.akka.io/docs/akka/2.5.6/scala/serialization.html#Customization) to serialize as JSON,
But make sure you extend `akka.persistence.eventstore.EventStoreSerializer` rather then `akka.serialization.Serializer`. 
And in case you are really going to serialize as json, please specify `ContentType.Json`, it will allow you to use projections.
 
```scala
trait EventStoreSerializer extends Serializer {
  def toEvent(o: AnyRef): EventData
  def fromEvent(event: Event, manifest: Class[_]): AnyRef
}
```
 
Please check out some real examples used in tests:
* [json4s](https://github.com/EventStore/EventStore.Akka.Persistence/blob/master/src/it/scala/akka/persistence/eventstore/Json4sSerializer.scala)
* [spray-json](https://github.com/EventStore/EventStore.Akka.Persistence/blob/master/src/it/scala/akka/persistence/eventstore/SprayJsonSerializer.scala)


## Setup

#### Sbt
```scala
libraryDependencies += "com.geteventstore" %% "akka-persistence-eventstore" % "5.0.1"
```

#### Maven
```xml
<dependency>
    <groupId>com.geteventstore</groupId>
    <artifactId>akka-persistence-eventstore_${scala.version}</artifactId>
    <version>5.0.1</version>
</dependency>
```

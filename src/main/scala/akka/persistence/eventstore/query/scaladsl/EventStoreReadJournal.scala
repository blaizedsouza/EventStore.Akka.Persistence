package akka.persistence.eventstore.query.scaladsl

import akka.NotUsed
import akka.actor.ExtendedActorSystem
import akka.event.Logging
import akka.persistence.PersistentRepr
import akka.persistence.eventstore.EventStoreSerialization
import akka.persistence.eventstore.Helpers._
import akka.persistence.query._
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import eventstore.{ EventNumber, EventStoreExtension, EventStream }

import scala.util.control.NonFatal

class EventStoreReadJournal(system: ExtendedActorSystem, config: Config)
    extends ReadJournal
    with PersistenceIdsQuery
    with CurrentPersistenceIdsQuery
    with EventsByPersistenceIdQuery
    with EventsByTagQuery
    with CurrentEventsByTagQuery
    with CurrentEventsByPersistenceIdQuery {

  private val serialization = EventStoreSerialization(system)
  private val log = Logging.getLogger(system, getClass)

  def currentPersistenceIds(): Source[String, NotUsed] = {
    persistenceIds(infinite = false) named "currentPersistenceIds"
  }

  def persistenceIds(): Source[String, NotUsed] = {
    persistenceIds(infinite = true) named "persistenceIds"
  }

  def eventsByPersistenceId(persistenceId: String, from: Long, to: Long): Source[EventEnvelope, NotUsed] = {
    eventsByPersistenceId(persistenceId, from, to, infinite = true)
      .named(s"eventsByPersistenceId-$persistenceId-$from-$to")
  }

  def currentEventsByPersistenceId(persistenceId: String, from: Long, to: Long): Source[EventEnvelope, NotUsed] = {
    eventsByPersistenceId(persistenceId, from, to, infinite = false)
      .named(s"currentEventsByPersistenceId-$persistenceId-$from-$to")
  }

  def eventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed] = {
    try {
      val seqNr = toSequenceNr(offset)
      eventsByPersistenceId(tag, seqNr, Long.MaxValue, infinite = true)
        .named(s"eventsByTag-$tag-$seqNr")
    } catch {
      case NonFatal(e) =>
        log.debug("Could not run eventsByTag [{}] query, due to: {}", tag, e.getMessage)
        Source.failed(e)
    }
  }

  def currentEventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed] = {
    try {
      val seqNr = toSequenceNr(offset)
      eventsByPersistenceId(tag, seqNr, Long.MaxValue, infinite = false)
        .named(s"currentEventsByTag-$tag-$seqNr")
    } catch {
      case NonFatal(e) =>
        log.debug("Could not run currentEventsByTag [{}] query, due to: {}", tag, e.getMessage)
        Source.failed(e)
    }
  }

  private def toSequenceNr(offset: Offset) = offset match {
    case Sequence(value) => value
    case NoOffset        => 0L
    case unsupported =>
      throw new IllegalArgumentException("EventStore does not support " + unsupported.getClass.getName + " offsets")
  }

  private def eventsByPersistenceId(persistenceId: String, from: Long, to: Long, infinite: Boolean): Source[EventEnvelope, akka.NotUsed] = {

    def eventsByPersistenceId(from: Option[EventNumber], to: EventNumber) = {
      val streamId = EventStream.Id(persistenceId)
      val publisher = connection.streamPublisher(
        streamId,
        fromNumberExclusive = from,
        infinite = infinite,
        resolveLinkTos = true
      )
      Source.fromPublisher(publisher)
        .takeWhile { _.record.number <= to }
        .map { x =>
          val sequenceNr = sequenceNumber(x.record.number)
          EventEnvelope(
            offset = Sequence(sequenceNr),
            persistenceId = persistenceId,
            sequenceNr = sequenceNr,
            event = serialization.deserialize[PersistentRepr](x).payload
          )
        }
    }

    eventsByPersistenceId(
      if (from == 0) None else Some(eventNumber(from)),
      if (to >= Long.MaxValue) EventNumber.Last else eventNumber(to)
    )
  }

  private def persistenceIds(infinite: Boolean): Source[String, akka.NotUsed] = {
    val streamId = EventStream.System.`$streams`
    val publisher = connection.streamPublisher(streamId, infinite = infinite, resolveLinkTos = true)
    Source.fromPublisher(publisher) map { x => x.streamId.streamId }
  }

  private def connection = EventStoreExtension(system).connection

}

object EventStoreReadJournal {
  final val Identifier: String = "eventstore.persistence.query"
}
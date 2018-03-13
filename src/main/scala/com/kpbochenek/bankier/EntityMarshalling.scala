package com.kpbochenek.bankier

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import org.json4s.JsonAST.JString
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.json4s.{CustomSerializer, DefaultFormats, Formats}

object EntityMarshalling {

  case class InstantSerializer[JavaTimeSerializers](format: DateTimeFormatter) extends CustomSerializer[Instant](_ => (
    { case JString(s) => format.parse(s, (temporal: TemporalAccessor) => Instant.from(temporal)) },
    { case time: Instant => JString(format.format(time)) }
  ))

  implicit val formats: Formats = DefaultFormats + InstantSerializer(DateTimeFormatter.ISO_INSTANT)

  implicit def entityJsonUnmarshaller[T : Manifest]: FromEntityUnmarshaller[T] =
    Unmarshaller
      .stringUnmarshaller
      .forContentTypes(ContentTypes.`application/json`)
      .map(json => parse(json).extract[T])


  implicit def entityJsonMarshaller[T <: AnyRef : Manifest]: ToEntityMarshaller[T] =
    Marshaller
      .withFixedContentType(ContentTypes.`application/json`) { entity =>
        HttpEntity(ContentTypes.`application/json`, write[T](entity))
      }
}

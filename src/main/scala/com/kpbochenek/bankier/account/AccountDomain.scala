package com.kpbochenek.bankier.account

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

object AccountDomain {

  // REST
  case class CreateAccountRequest(login: String, password: String)
  case class CreateAccountResponse(login: String, id: String, balance: Int, createdAt: Instant)

  case class AccountResponse(login: String, id: String, balance: Int, createdAt: Instant)

  // INTERNAL
  case class Account(login: String, passwordHash: String, id: String, createdAt: Instant, balance: Int)
}

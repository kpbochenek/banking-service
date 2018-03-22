package com.kpbochenek.bankier.account

import java.time.Instant

import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.{ContentTypes, MessageEntity}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.kpbochenek.bankier.transfer.TransactionDomain.{AccountTransactions, Transaction}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class MarshallingTest extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach {
  import com.kpbochenek.bankier.EntityMarshalling._
  import com.kpbochenek.bankier.account.AccountDomain._

  "AccountMarshalling" should {
    "marshal domain objects" in {
      val now = Instant.now()
      verifyJsonSame(CreateAccountRequest("Ala", "secret"),
        """{"login":"Ala","password":"secret"}""")
      verifyJsonSame(CreateAccountResponse("Ala", "AF32-FF9E-91BB", 981, now),
        s"""{"login":"Ala","id":"AF32-FF9E-91BB","balance":981,"createdAt":"${now.toString}"}""")
      verifyJsonSame(AccountResponse("Ala", "AF32-FF9E-91BB", 234, now),
        s"""{"login":"Ala","id":"AF32-FF9E-91BB","balance":234,"createdAt":"${now.toString}"}""")

      verifyJsonSame(AccountTransactions(List(Transaction("TX-1", "FROM-ACC-1", "TO-ACC-1", 100))),
        s"""{"transactions":[{"transactionId":"TX-1","fromAccountId":"FROM-ACC-1","toAccountId":"TO-ACC-1","amount":100}]}""")
    }

  }

  private def verifyJsonSame[T](message: T, expectedJson: String)(implicit m: Marshaller[T, MessageEntity]): Unit = {
    val entity = wait(Marshal(message).to[MessageEntity])
    entity.contentType shouldEqual ContentTypes.`application/json`
    val unmarshalledJson = wait(entity.toStrict(1.second).map(_.data.utf8String))
    unmarshalledJson shouldEqual expectedJson

  }

  private def wait[T](f: Future[T]): T = Await.result(f, 1.second)
}

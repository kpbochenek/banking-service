package com.kpbochenek.bankier.account

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.kpbochenek.bankier.persistence.DatabasePersistence
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import slick.jdbc.H2Profile.api._

import scala.collection.mutable


class AccountRouteTest extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach {
  import AccountDomain._
  import com.kpbochenek.bankier.EntityMarshalling._

  var underTest: AccountRoute = _
  var db: Database = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    db = Database.forConfig("h2mem")
    underTest = new AccountRoute(new AccountLogic(new DatabasePersistence(db)))
  }

  override def afterEach(): Unit = {
    super.afterEach()
    db.close()
  }


  val accountPath = "/accounts"
  def accountPathForLogin(login: String): String = accountPath + "/" + login

  val createAccountRequest = CreateAccountRequest("login", "password")

  "AccountHandler AccountCreation" should {
    "create account successfully" in {
      Post(accountPath, createAccountRequest) ~> underTest.routes ~> check {
        println(response)
        status shouldEqual StatusCodes.OK
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[CreateAccountResponse].balance shouldEqual 0
        responseAs[CreateAccountResponse].login shouldEqual createAccountRequest.login
        responseAs[CreateAccountResponse].createdAt should not be null
      }
    }

    "fail at creating account second time" in {
      Post(accountPath, createAccountRequest) ~> underTest.routes ~> check {
        println(response)
        status shouldEqual StatusCodes.OK
      }

      Post(accountPath, createAccountRequest) ~> underTest.routes ~> check {
        println(response)
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual "Account already exists 'login'"
      }
    }

    "different accounts should have random account ids" in {
      val request = CreateAccountRequest("A", "A1")
      var accountAId: String = null
      Post(accountPath, request) ~> underTest.routes ~> check {
        println(response)
        status shouldEqual StatusCodes.OK
        accountAId = responseAs[CreateAccountResponse].id
      }

      val request2 = CreateAccountRequest("B", "B2")
      Post(accountPath, request2) ~> underTest.routes ~> check {
        println(response)
        status shouldEqual StatusCodes.OK
        responseAs[CreateAccountResponse].id should not equal accountAId
      }
    }

    "AccountHandler GetAccount" should {
      "return not found when account not found" in {
        Get(accountPathForLogin("RandomLogin")) ~> underTest.routes ~> check {
          status shouldEqual StatusCodes.NotFound
        }
      }

      "return correct account with default balance" in {
        Get(accountPathForLogin(createAccountRequest.login)) ~> underTest.routes ~> check {
          status shouldEqual StatusCodes.NotFound
        }

        var accountId: String = null
        Post(accountPath, createAccountRequest) ~> underTest.routes ~> check {
          status shouldEqual StatusCodes.OK
          accountId = responseAs[CreateAccountResponse].id
        }

        Get(accountPathForLogin(createAccountRequest.login)) ~> underTest.routes ~> check {
          println(response)
          status shouldEqual StatusCodes.OK
          responseAs[AccountResponse].login shouldEqual createAccountRequest.login
          responseAs[AccountResponse].balance shouldEqual 0
          responseAs[AccountResponse].id shouldEqual accountId
        }
      }

      "properly return different accounts" in {
        val accountIds: mutable.Map[Int, String] = mutable.Map()
        for (i <- 1 to 10) {
          Post(accountPath, CreateAccountRequest(i.toString, "password")) ~> underTest.routes ~> check {
            status shouldEqual StatusCodes.OK
            accountIds.update(i, responseAs[CreateAccountResponse].id)
          }
        }

        for (i <- 1 to 10) {
          Get(accountPathForLogin(i.toString)) ~> underTest.routes ~> check {
            status shouldEqual StatusCodes.OK
            responseAs[AccountResponse].id shouldEqual accountIds(i)
          }
        }
      }
    }

    "AccountHandler DeleteAccount" should {
      "ignore request when account not present" in {
        Delete(accountPathForLogin("UnknownLogin")) ~> underTest.routes ~> check {
          status shouldEqual StatusCodes.NotFound
        }
      }

      "delete account correctly" in {
        Post(accountPath, createAccountRequest) ~> underTest.routes ~> check {
          status shouldEqual StatusCodes.OK
        }

        Get(accountPathForLogin(createAccountRequest.login)) ~> underTest.routes ~> check {
          status shouldEqual StatusCodes.OK
        }

        Delete(accountPathForLogin(createAccountRequest.login)) ~> underTest.routes ~> check {
          status shouldEqual StatusCodes.OK
        }

        Get(accountPathForLogin(createAccountRequest.login)) ~> underTest.routes ~> check {
          status shouldEqual StatusCodes.NotFound
        }
      }

    }
  }
}
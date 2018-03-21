package com.kpbochenek.bankier.account

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.kpbochenek.bankier.persistence.DatabasePersistence
import com.kpbochenek.bankier.transfer.TransactionDomain.{DepositRequest, TransactionRequest, WithdrawRequest}
import com.kpbochenek.bankier.transfer.{TransactionLogic, TransactionRoute}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class TransactionRouteTest extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach {

  import AccountDomain._
  import com.kpbochenek.bankier.EntityMarshalling._

  var underTest: TransactionRoute = _
  var db: Database = _

  var accountLogic: AccountLogic = _
  var accountIds: List[String] = _


  override def beforeEach(): Unit = {
    super.beforeEach()
    db = Database.forConfig("h2mem")
    val persistence = new DatabasePersistence(db)
    underTest = new TransactionRoute(new TransactionLogic(persistence, persistence))

    accountLogic = new AccountLogic(persistence)
    accountIds = (1 to 2)
      .map(i => wait(accountLogic.createAccount(CreateAccountRequest(s"AC-$i", "PASSWORD"))))
      .collect { case x: Right[String, CreateAccountResponse] => x.value.id }
      .toList
  }

  override def afterEach(): Unit = {
    super.afterEach()
    db.close()
  }

  val depositPath = "/transactions/deposit"
  val withdrawPath = "/transactions/withdraw"
  val transactionPath = "/transactions/transaction"

  val createAccountRequest = CreateAccountRequest("login", "password")

  "TransferHandler deposit" should {
    "deposit money successfully" in {
      executeDeposit("TX-1", accountIds.head, 10, StatusCodes.OK)
      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 10
    }

    "sum deposited money" in {
      executeDeposit("TX-1", accountIds.head, 33, StatusCodes.OK)
      executeDeposit("TX-2", accountIds.head, 18, StatusCodes.OK)
      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 51
    }

    "ignore deposit for unknown account" in {
      executeDeposit("TX-1", "UNKNOWN-ACCOUNT", 10, StatusCodes.BadRequest)
    }

    "ignore depositing money second time" in {
      executeDeposit("TX-1", accountIds.head, 15, StatusCodes.OK)
      executeDeposit("TX-1", accountIds.head, 18, StatusCodes.BadRequest)
      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 15
    }
  }

  "TransferHandler withdraw" should {
    "withdraw money successfully" in {
      executeDeposit("TX-0", accountIds.head, 100, StatusCodes.OK)
      executeWithdraw("TX-1", accountIds.head, 10, StatusCodes.OK)
      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 90
    }

    "sum withdraw money" in {
      executeDeposit("TX-0", accountIds.head, 100, StatusCodes.OK)
      executeWithdraw("TX-1", accountIds.head, 20, StatusCodes.OK)
      executeWithdraw("TX-2", accountIds.head, 18, StatusCodes.OK)
      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 62
    }

    "ignore withdraw for unknown account" in {
      executeWithdraw("TX-1", "UNKNOWN-ACCOUNT", 10, StatusCodes.BadRequest)
    }

    "ignore withdraw money second time" in {
      executeDeposit("TX-0", accountIds.head, 100, StatusCodes.OK)
      executeWithdraw("TX-1", accountIds.head, 15, StatusCodes.OK)
      executeWithdraw("TX-1", accountIds.head, 50, StatusCodes.BadRequest)
      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 85
    }

    "fail withdrawing money with insufficient funds" in {
      executeDeposit("TX-1", accountIds.head, 10, StatusCodes.OK)
      executeWithdraw("TX-2", accountIds.head, 12, StatusCodes.BadRequest)
      executeWithdraw("TX-3", accountIds.head, 10, StatusCodes.OK)
      executeWithdraw("TX-4", accountIds.head, 1, StatusCodes.BadRequest)
    }
  }

  "TransferHandler" should {
    "deposit then withdraw correctly" in {
      executeDeposit("TX-1", accountIds.head, 200, StatusCodes.OK)
      executeWithdraw("TX-2", accountIds.head, 50, StatusCodes.OK)

      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 150
    }

    "withdraw then deposit correctly" in {
      executeDeposit("TX-0", accountIds.head, 100, StatusCodes.OK)
      executeWithdraw("TX-1", accountIds.head, 50, StatusCodes.OK)
      executeDeposit("TX-2", accountIds.head, 200, StatusCodes.OK)

      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 250
    }
  }

  "TransactionHandler transaction" should {
    "successfully make transaction" in {
      executeDeposit("TX-1", accountIds(0), 200, StatusCodes.OK)
      executeDeposit("TX-2", accountIds(1), 300, StatusCodes.OK)

      executeTransfer("TX-3", accountIds(0), accountIds(1), 50, StatusCodes.OK)

      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 150
      wait(accountLogic.getAccountByLogin("AC-2")).get.balance shouldEqual 350
    }

    "fail transaction when first account not present" in {
      executeDeposit("TX-1", accountIds(0), 200, StatusCodes.OK)
      executeDeposit("TX-2", accountIds(1), 300, StatusCodes.OK)

      executeTransfer("TX-3", accountIds(0), "UNKNOWN-ID", 50, StatusCodes.BadRequest)

      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 200
      wait(accountLogic.getAccountByLogin("AC-2")).get.balance shouldEqual 300
    }

    "fail transaction when second account not present" in {
      executeDeposit("TX-1", accountIds(0), 200, StatusCodes.OK)
      executeDeposit("TX-2", accountIds(1), 300, StatusCodes.OK)

      executeTransfer("TX-3", "UNKNOWN-ID", accountIds(1), 50, StatusCodes.BadRequest)

      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 200
      wait(accountLogic.getAccountByLogin("AC-2")).get.balance shouldEqual 300

    }

    "ignore transaction when already finished" in {
      executeDeposit("TX-1", accountIds(0), 200, StatusCodes.OK)
      executeDeposit("TX-2", accountIds(1), 300, StatusCodes.OK)

      executeTransfer("TX-3", accountIds(0), accountIds(1), 50, StatusCodes.OK)
      executeTransfer("TX-3", accountIds(0), accountIds(1), 50, StatusCodes.BadRequest)
      executeTransfer("TX-3", accountIds(0), accountIds(1), 50, StatusCodes.BadRequest)

      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 150
      wait(accountLogic.getAccountByLogin("AC-2")).get.balance shouldEqual 350
    }

    "fail transaction when account has insufficient funds" in {
      executeDeposit("TX-1", accountIds(0), 100, StatusCodes.OK)
      executeTransfer("TX-2", accountIds(0), accountIds(1), 200, StatusCodes.BadRequest)
      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 100
      wait(accountLogic.getAccountByLogin("AC-2")).get.balance shouldEqual 0
    }

    "fail transaction when account has insufficient funds advanced" in {
      executeDeposit("TX-1", accountIds(0), 100, StatusCodes.OK)
      executeTransfer("TX-2", accountIds(0), accountIds(1), 50, StatusCodes.OK)
      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 50
      wait(accountLogic.getAccountByLogin("AC-2")).get.balance shouldEqual 50

      executeTransfer("TX-3", accountIds(0), accountIds(1), 60, StatusCodes.BadRequest)
      executeTransfer("TX-4", accountIds(0), accountIds(1), 50, StatusCodes.OK)

      wait(accountLogic.getAccountByLogin("AC-1")).get.balance shouldEqual 0
      wait(accountLogic.getAccountByLogin("AC-2")).get.balance shouldEqual 100

    }
  }

  private def wait[T](f: Future[T]): T = Await.result(f, 1.second)

  private def executeDeposit(txId: String, accountId: String, amount: Int, expectedStatus: StatusCode): Unit = {
    Post(depositPath, DepositRequest(txId, accountId, amount)) ~> underTest.routes ~> check {
      status shouldEqual expectedStatus
    }
  }

  private def executeWithdraw(txId: String, accountId: String, amount: Int, expectedStatus: StatusCode): Unit = {
    Post(withdrawPath, WithdrawRequest(txId, accountId, amount)) ~> underTest.routes ~> check {
      status shouldEqual expectedStatus
    }
  }

  private def executeTransfer(txId: String, fromId: String, toId: String, amount: Int, expectedStatus: StatusCode): Unit = {
    Post(transactionPath, TransactionRequest(txId, fromId, toId, amount)) ~> underTest.routes ~> check {
      status shouldEqual expectedStatus
    }
  }
}
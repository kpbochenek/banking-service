package com.kpbochenek.bankier.persistence
import com.kpbochenek.bankier.account.AccountDomain._
import com.kpbochenek.bankier.transfer.TransactionDomain.Transaction
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class DatabasePersistence(db: Database) extends AccountPersistence with TransactionPersistence {
  import com.kpbochenek.bankier.persistence.AccountTable.accounts
  import TransactionTable.{transactions, _}

  val BOTTOM_ACCOUNT_LIMIT = 0

  Await.result(db.run(DBIO.seq(accounts.schema.create, transactions.schema.create)), 5.seconds)

  override def addAccount(account: Account): Future[Unit] =
    db.run(accounts += account)
      .map(_ => Unit)

  override def getAccountByLogin(login: String): Future[Option[Account]] = {
    db.run(accounts.filter(_.login === login).result.headOption)
  }

  override def getAccountById(id: String): Future[Option[Account]] = {
    db.run(accounts.filter(_.id === id).result.headOption)
  }

  override def removeAccount(login: String): Future[Boolean] =
    db.run(accounts.filter(_.login === login).delete).map(_ > 0)

  override def isAccountPresent(login: String): Future[Boolean] =
    db.run(accounts.filter(_.login === login).exists.result)


  override def isTransactionProcessed(transactionId: String): Future[Boolean] = {
    db.run(transactions.filter(_.transactionId === transactionId).exists.result)
  }

  override def updateAccountBalance(transactionId: String, accountId: String, amount: Int): Future[Boolean] = {
    val tr = if (amount < 0) {
      Transaction(transactionId, accountId, VOID_ACCOUNT, amount)
    } else {
      Transaction(transactionId, VOID_ACCOUNT, accountId, amount)
    }

    val updateTransaction = (for {
      balance <- accounts.filter(_.id === accountId).map(_.balance).result.head
      if balance + amount >= BOTTOM_ACCOUNT_LIMIT
      _ <- transactions += tr
      _ <- accounts.filter(_.id === accountId).map(_.balance).update(balance + amount)
    } yield true).transactionally
    db.run(updateTransaction)
      .recoverWith { case _: NoSuchElementException => Future.successful(false)}
  }

  override def makeTransaction(transactionId: String, fromAccountId: String, toAccountId: String, amount: Int): Future[Boolean] = {
    val transferTransaction = (for {
      fromBalance <- accounts.filter(_.id === fromAccountId).map(_.balance).result.head
      toBalance <- accounts.filter(_.id === toAccountId).map(_.balance).result.head
      if fromBalance - amount >= BOTTOM_ACCOUNT_LIMIT
      _ <- transactions += Transaction(transactionId, fromAccountId, toAccountId, amount)
      _ <- accounts.filter(_.id === fromAccountId).map(_.balance).update(fromBalance - amount)
      _ <- accounts.filter(_.id === toAccountId).map(_.balance).update(toBalance + amount)
    } yield true).transactionally
    db.run(transferTransaction)
      .recoverWith { case _: NoSuchElementException => Future.successful(false)}
  }
}

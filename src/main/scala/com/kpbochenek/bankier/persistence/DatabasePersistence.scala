package com.kpbochenek.bankier.persistence
import com.kpbochenek.bankier.account.AccountDomain._
import com.kpbochenek.bankier.transfer.TransactionDomain.Transaction
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class DatabasePersistence(db: Database) extends AccountPersistence with TransactionPersistence {
  import com.kpbochenek.bankier.persistence.AccountTable.accounts
  import com.kpbochenek.bankier.persistence.TransactionTable.transactions

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

  override def logTransaction(transactionId: String, fromAccountId: String, toAccountId: String, amount: Int): Future[Unit] = {
    db.run(transactions += Transaction(transactionId, fromAccountId, toAccountId, amount))
      .map(_ => Unit)
  }

  override def updateAccountBalance(transactionId: String, accountId: String, amount: Int): Future[Unit] = {
    db.run(accounts.filter(_.id === accountId).map(_.balance).update(amount))
      .map(_ => Unit)
  }

  override def makeTransaction(transactionId: String, fromAccountId: String, toAccountId: String, amount: Int): Future[Unit] = {
    db.run(DBIO.seq(
      sqlu"update account SET balance = balance - $amount WHERE id=$fromAccountId",
      sqlu"update account SET balance = balance + $amount WHERE id=$toAccountId"
    ))
  }
}

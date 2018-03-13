package com.kpbochenek.bankier.persistence

import scala.concurrent.Future

trait TransactionPersistence {
  def isTransactionProcessed(transactionId: String): Future[Boolean]

  def logTransaction(transactionId: String, fromAccountId: String, toAccountId: String, amount: Int): Future[Unit]

  def updateAccountBalance(transactionId: String, accountId: String, amount: Int): Future[Unit]

  def makeTransaction(transactionId: String, fromAccountId: String, toAccountId: String, amount: Int): Future[Unit]
}

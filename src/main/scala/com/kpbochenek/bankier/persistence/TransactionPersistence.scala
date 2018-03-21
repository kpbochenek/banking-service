package com.kpbochenek.bankier.persistence

import scala.concurrent.Future

trait TransactionPersistence {
  def isTransactionProcessed(transactionId: String): Future[Boolean]

  def updateAccountBalance(transactionId: String, accountId: String, amount: Int): Future[Boolean]

  def makeTransaction(transactionId: String, fromAccountId: String, toAccountId: String, amount: Int): Future[Boolean]
}

package com.kpbochenek.bankier.transfer

import com.kpbochenek.bankier.account.AccountDomain.Account
import com.kpbochenek.bankier.persistence.{AccountPersistence, TransactionPersistence}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class TransactionLogic(transactionPersistence: TransactionPersistence, accountPersistence: AccountPersistence) extends LazyLogging {
  import TransactionDomain._

  type TransactionResult = Either[TransactionError, TransactionSuccess]

  def transferMoney(transactionId: String, fromId: String, toId: String, amount: Int): Future[TransactionResult] = {
    accountPersistence.getAccountById(fromId).flatMap {
      case Some(fromAccount) =>
        accountPersistence.getAccountById(toId).flatMap {
          case Some(toAccount) =>
            transactionPersistence.isTransactionProcessed(transactionId).flatMap(isAlreadyProcessed => {
              if (isAlreadyProcessed) {
                Future.successful(transactionFailed(s"Transaction '$transactionId' already processed."))
              } else {
                transferMoney(transactionId, fromAccount, toAccount, amount)
              }
            })
          case None => Future.successful(transactionFailed(s"Account not found $toId"))
        }
      case None => Future.successful(transactionFailed(s"Account not found $fromId"))
    }
  }

  private def transactionFailed(error: String): TransactionResult = Left(TransactionError(error))

  def depositMoney(transactionId: String, accountId: String, amount: Int): Future[TransactionResult] = {
    logger.info(s"Changing account $accountId with amount $amount")
    val accountF = accountPersistence.getAccountById(accountId)
    accountF.flatMap {
      case Some(account) =>
        transactionPersistence.isTransactionProcessed(transactionId).flatMap(isAlreadyProcessed => {
          if (isAlreadyProcessed) {
            Future.successful(transactionFailed(s"Transaction '$transactionId' already processed."))
          } else {
            makeDeposit(transactionId, account.id, amount)
          }
        })
      case None => Future.successful(transactionFailed(s"Account not found $accountId"))
    }
  }

  private def makeDeposit(transactionId: String, accountId: String, amount: Int): Future[TransactionResult] = {
    transactionPersistence.updateAccountBalance(transactionId, accountId, amount).map(success => {
        if (success) {
          Right(TransactionSuccess())
        } else {
          transactionFailed("Insufficient funds")
        }
      })
  }

  def withdrawMoney(transactionId: String, accountId: String, amount: Int): Future[TransactionResult] =
    depositMoney(transactionId, accountId, -amount)

  def getHistory(accountId: String): Future[Option[List[Transaction]]] = {
    logger.info(s"Fetching history of account $accountId")
    val accountF = accountPersistence.getAccountById(accountId)
    accountF.flatMap {
      case Some(_) => transactionPersistence.getHistory(accountId).map(t => Some(t))
      case None => Future.successful(None)
    }
  }

  private def transferMoney(transactionId: String, fromAccount: Account, toAccount: Account, amount: Int): Future[TransactionResult] = {
    transactionPersistence.makeTransaction(transactionId, fromAccount.id, toAccount.id, amount).map(success => {
      if (success) {
        Right(TransactionSuccess())
      } else {
        transactionFailed("Insufficient funds")
      }
    })
  }
}

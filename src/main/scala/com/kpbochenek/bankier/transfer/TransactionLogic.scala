package com.kpbochenek.bankier.transfer

import com.kpbochenek.bankier.account.AccountDomain.Account
import com.kpbochenek.bankier.persistence.TransactionTable.VOID_ACCOUNT
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
            for {
              _ <- transactionPersistence.updateAccountBalance(transactionId, account.id, account.balance + amount)
              _ <- transactionPersistence.logTransaction(transactionId, VOID_ACCOUNT, account.id, amount)
            } yield Right(TransactionSuccess())
          }
        })
      case None =>
        Future.successful(Left(TransactionError(s"Account not found $accountId")))
    }
  }

  def withdrawMoney(transactionId: String, accountId: String, amount: Int): Future[TransactionResult] =
    depositMoney(transactionId, accountId, -amount)


  private def transferMoney(transactionId: String, fromAccount: Account, toAccount: Account, amount: Int): Future[TransactionResult] = {
    for {
      _ <- transactionPersistence.makeTransaction(transactionId, fromAccount.id, toAccount.id, amount)
      _ <- transactionPersistence.logTransaction(transactionId, fromAccount.id, toAccount.id, amount)
    } yield Right(TransactionSuccess())
  }

}

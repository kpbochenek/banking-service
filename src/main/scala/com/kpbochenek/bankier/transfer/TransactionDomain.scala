package com.kpbochenek.bankier.transfer

object TransactionDomain {

  // REST
  case class TransactionRequest(transactionId: String, fromId: String, toId: String, amount: Int)

  case class DepositRequest(transactionId: String, accountId: String, amount: Int)

  case class WithdrawRequest(transactionId: String, accountId: String, amount: Int)

  case class TransactionSuccess()
  case class TransactionError(error: String)

  // INTERNAL
  case class Transaction(transactionId: String, fromAccountId: String, toAccountId: String, amount: Int)
}

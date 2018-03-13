package com.kpbochenek.bankier.persistence

import com.kpbochenek.bankier.transfer.TransactionDomain.Transaction
import slick.jdbc.H2Profile.api._
import slick.lifted.Tag


class TransactionTable(tag: Tag) extends Table[Transaction](tag, "transactions") {

  def transactionId = column[String]("transactionId", O.PrimaryKey)
  def fromAccountId = column[String]("fromAccountId")
  def toAccountId = column[String]("toAccountId")
  def amount = column[Int]("amount")

  def * = (transactionId, fromAccountId, toAccountId, amount) <> (Transaction.tupled, Transaction.unapply)
}


object TransactionTable {
  val VOID_ACCOUNT = "VOID"

  val transactions = TableQuery[TransactionTable]
}
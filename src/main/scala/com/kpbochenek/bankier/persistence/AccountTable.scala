package com.kpbochenek.bankier.persistence

import java.sql.Timestamp
import java.time.Instant

import com.kpbochenek.bankier.account.AccountDomain.Account
import slick.jdbc.H2Profile.api._

class AccountTable(tag: Tag) extends Table[Account](tag, "account") {
  implicit val JavaInstantTimeMapper = MappedColumnType.base[Instant, Timestamp](
    instant => Timestamp.from(instant),
    timestamp => timestamp.toInstant
  )

  def login = column[String]("login", O.PrimaryKey) // This is the primary key column
  def passwordHash = column[String]("passwordHash")
  def id = column[String]("id", O.Unique)
  def createdAt = column[Instant]("createdAt")
  def balance = column[Int]("balance")
  def * = (login, passwordHash, id, createdAt, balance) <> (Account.tupled, Account.unapply)
}


object AccountTable {
  val accounts = TableQuery[AccountTable]
}
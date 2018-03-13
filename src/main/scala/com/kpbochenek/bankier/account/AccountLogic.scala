package com.kpbochenek.bankier.account

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

import com.kpbochenek.bankier.account.AccountDomain._
import com.kpbochenek.bankier.persistence.AccountPersistence
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountLogic(persistence: AccountPersistence) extends LazyLogging {


  def createAccount(request: CreateAccountRequest): Future[Either[String, CreateAccountResponse]] = {
    val id = UUID.randomUUID().toString
    val balance = 0
    val createdAt = Instant.now()
    val passwordHash = new String(MessageDigest.getInstance("SHA-256").digest("password".getBytes("UTF-8")))
    val account = Account(request.login, passwordHash, id, createdAt, balance)

    persistence.isAccountPresent(request.login).flatMap(isAccountPresent =>
      if (isAccountPresent) {
        Future.successful(Left(s"Account already exists '${request.login}'"))
      } else {
        logger.info(s"Creating account for login '${request.login}' with id '$id' at '$createdAt'")
        persistence.addAccount(account).map(_ =>
          Right(CreateAccountResponse(request.login, id, balance, createdAt))
        )
      }
    )
  }

  def getAccountByLogin(login: String): Future[Option[Account]] = persistence.getAccountByLogin(login)

  def deleteAccount(login: String): Future[Boolean] = persistence.removeAccount(login)

  def toAccountResponse(account: Account): AccountResponse =
    AccountResponse(account.login, account.id, account.balance, account.createdAt)
}

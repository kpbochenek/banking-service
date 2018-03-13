package com.kpbochenek.bankier.persistence

import com.kpbochenek.bankier.account.AccountDomain.Account

import scala.concurrent.Future

trait AccountPersistence {
  def addAccount(account: Account): Future[Unit]
  def getAccountByLogin(login: String): Future[Option[Account]]
  def getAccountById(id: String) : Future[Option[Account]]
  def removeAccount(login: String): Future[Boolean]
  def isAccountPresent(login: String): Future[Boolean]
}

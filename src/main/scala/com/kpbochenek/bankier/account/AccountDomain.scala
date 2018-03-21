package com.kpbochenek.bankier.account

import java.time.Instant

object AccountDomain {

  // REST
  case class CreateAccountRequest(login: String, password: String)
  case class CreateAccountResponse(login: String, id: String, balance: Int, createdAt: Instant)

  case class AccountResponse(login: String, id: String, balance: Int, createdAt: Instant)

  // INTERNAL
  case class Account(login: String, passwordHash: String, id: String, createdAt: Instant, balance: Int)
}

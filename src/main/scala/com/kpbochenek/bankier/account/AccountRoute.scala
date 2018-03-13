package com.kpbochenek.bankier.account

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations._
import javax.ws.rs.Path

@Api(value = "/accounts")
@Path("/accounts")
class AccountRoute(accountLogic: AccountLogic) extends Directives with LazyLogging {
  import com.kpbochenek.bankier.EntityMarshalling._
  import com.kpbochenek.bankier.account.AccountDomain._

  def routes: Route =
    pathPrefix("accounts") {
      createAccount ~ getAccount ~ deleteAccount
    }

  @ApiOperation(httpMethod = "POST", value = "Creates bank account", consumes = "application/json", produces = "application/json")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Account created successfully"),
    new ApiResponse(code = 400, message = "Account already exists")
  ))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Create Account Request", required = true, dataTypeClass = classOf[CreateAccountRequest], paramType = "body")
  ))
  def createAccount: Route =
      pathEndOrSingleSlash {
        (post & entity(as[CreateAccountRequest])) { request =>
          onSuccess(accountLogic.createAccount(request)) {
            case Right(account) => complete(account)
            case Left(error) => complete(HttpResponse(StatusCodes.BadRequest, entity = error))
          }
        }
    }


  @ApiOperation(httpMethod = "GET", value = "Get bank account details", consumes = "application/json", produces = "application/json")
  @Path("/{login}")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "login", value = "Login of account to be retrieved", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Account details", response = classOf[AccountResponse]),
    new ApiResponse(code = 404, message = "Account not found")
  ))
  def getAccount: Route =
    (get & path(Segment)) { login =>
      onSuccess(accountLogic.getAccountByLogin(login)) {
        case Some(account) => complete(accountLogic.toAccountResponse(account))
        case None => complete(HttpResponse(StatusCodes.NotFound))
      }
    }


  @ApiOperation(httpMethod = "DELETE", value = "delete bank account")
  @Path("/{login}")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "login", value = "Account Login to be deleted", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Account deleted successfully!"),
  ))
  def deleteAccount: Route =
    (delete & path(Segment)) { login =>
      onSuccess(accountLogic.deleteAccount(login)) {
        case true => complete(StatusCodes.OK)
        case false => complete(StatusCodes.NotFound)
      }
    }
}
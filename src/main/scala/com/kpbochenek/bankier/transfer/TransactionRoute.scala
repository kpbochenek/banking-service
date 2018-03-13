package com.kpbochenek.bankier.transfer

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations._
import javax.ws.rs.Path


@Api(value = "/transactions")
@Path("/transactions")
class TransactionRoute(val transferLogic: TransactionLogic) extends Directives with LazyLogging {
  import TransactionDomain._
  import com.kpbochenek.bankier.EntityMarshalling._


  def routes: Route =
    (pathPrefix("transactions") & post) {
      transaction ~ deposit ~ withdraw
    }


  @Path("/transaction")
  @ApiOperation(httpMethod = "POST", value = "Transfer money between accounts", consumes = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "TransactionRequest", required = true, dataTypeClass = classOf[TransactionRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Transaction success"),
    new ApiResponse(code = 400, message = "Bad request")
  ))
  def transaction: Route =
    pathPrefix("transaction") {
      entity(as[TransactionRequest]) { req =>
        onSuccess(transferLogic.transferMoney(req.transactionId, req.fromId, req.toId, req.amount))(handleTransferResult)
      }
    }


  @Path("/deposit")
  @ApiOperation(httpMethod = "POST", value = "Deposit money to account", consumes = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "DepositRequest", required = true, dataTypeClass = classOf[DepositRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Transaction success"),
    new ApiResponse(code = 400, message = "Bad request")
  ))
  def deposit: Route =
    pathPrefix("deposit") {
      entity(as[DepositRequest]) { req =>
        onSuccess(transferLogic.depositMoney(req.transactionId, req.accountId, req.amount))(handleTransferResult)
      }
    }


  @Path("/withdraw")
  @ApiOperation(httpMethod = "POST", value = "Withdraw money from account", consumes = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "WithdrawRequest", required = true, dataTypeClass = classOf[WithdrawRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Transaction success"),
    new ApiResponse(code = 400, message = "Bad request")
  ))
  def withdraw: Route =
    pathPrefix("withdraw") {
      entity(as[WithdrawRequest]) { req =>
        onSuccess(transferLogic.withdrawMoney(req.transactionId, req.accountId, req.amount))(handleTransferResult)
      }
    }

  private def handleTransferResult(result: Either[TransactionError, TransactionSuccess]): Route = result match {
    case Right(TransactionSuccess()) => complete(StatusCodes.OK)
    case Left(TransactionError(error)) => complete(HttpResponse(StatusCodes.BadRequest, entity = error))
  }
}
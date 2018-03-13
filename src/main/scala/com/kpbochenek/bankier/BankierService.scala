package com.kpbochenek.bankier

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import com.kpbochenek.bankier.account.{AccountLogic, AccountRoute}
import com.kpbochenek.bankier.persistence.DatabasePersistence
import com.kpbochenek.bankier.transfer.{TransactionLogic, TransactionRoute}
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContextExecutor


object BankierService extends LazyLogging with Directives {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("bankier")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val persistence = new DatabasePersistence(Database.forConfig("h2mem"))
    val accountLogic = new AccountLogic(persistence)
    val accounts = new AccountRoute(accountLogic)

    val transferLogic = new TransactionLogic(persistence, persistence)
    val transferRoute = new TransactionRoute(transferLogic)

    val route = accounts.routes ~ transferRoute.routes ~ SwaggerDocService.routes ~ SwaggerDocService.staticApiRoutes

    Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress Ctrl-C to stop...")
  }
}

object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set(classOf[AccountRoute], classOf[TransactionRoute])
  override val host = "localhost:8080" //the url of your api, not swagger's json endpoint
  override val basePath = "/"    //the basePath for the API you are exposing
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info() //provides license and other description details
  override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")

  val staticApiRoutes = path("api") { getFromResource("swagger/index.html") } ~
    getFromResourceDirectory("swagger")
}
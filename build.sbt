import sbtassembly.MergeStrategy

name := "Banking Service"
organization := "com.kpbochenek"
description := "Simple transaction management software."

scalaVersion := "2.12.4"

test in assembly := {}

assemblyJarName in assembly := "bankier.jar"

parallelExecution in Test := false

enablePlugins(DockerPlugin)

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

buildOptions in docker := BuildOptions(cache = false)

val akkaV = "2.5.11"
val akkaHttpV = "10.1.0"
val scalatestV = "3.0.5"
val json4sV = "3.5.3"
val loggingV = "3.8.0"
val slickV = "3.2.2"
val h2driverV = "1.4.196"
val logbackV = "1.2.3"
val akkaSwaggerV = "0.14.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,

  "org.json4s" %% "json4s-native" % json4sV,
  "org.json4s" %% "json4s-ext" % json4sV,

  "com.typesafe.scala-logging" %% "scala-logging" % loggingV,
  "ch.qos.logback" % "logback-classic" % logbackV,

  "com.typesafe.slick" %% "slick" % slickV,
  "com.h2database" % "h2" % h2driverV,

  "com.github.swagger-akka-http" %% "swagger-akka-http" % akkaSwaggerV,

  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
  "org.scalatest" %% "scalatest" % scalatestV % Test
)


val defaultMergeStrategy: String => MergeStrategy = {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case _ => MergeStrategy.deduplicate
}
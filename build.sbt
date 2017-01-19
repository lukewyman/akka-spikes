lazy val `akka-spikes` = project.in(file("."))
  .aggregate(`http-client`,  routing)

lazy val `akka-stream-kafka` = project.in(file("akka-stream-kafka")).settings(Seq(
  scalaVersion := "2.11.8",
  assemblyJarName in assembly := "akka-stream-kafka.jar",
  libraryDependencies ++= Seq(
    Library.akkaActor,
    Library.akkaSlf4j,
    Library.akkaStreamKafka
  )
))

lazy val futures = project.in(file("futures")).settings(Seq(
  scalaVersion :=  "2.11.8",
  assemblyJarName in assembly := "futures",
  libraryDependencies ++= Seq(
    Library.akkaActor,
    Library.akkaSlf4j,
    Library.cats
  )
))

lazy val `http-client` = project.in(file("http-client")).settings(Seq(
  scalaVersion := "2.11.8",
  assemblyJarName in assembly := "http-client.jar",
  libraryDependencies ++= Seq(
    Library.akkaActor,
    Library.akkaSlf4j,
    Library.akkaHttp,
    Library.akkaHttpCirce,
    Library.akkaSprayJson,
    Library.circeCore,
    Library.circeGeneric,
    Library.circeParser
  )
))

lazy val routing = project.in(file("routing")).settings(Seq(
  scalaVersion := Version.scala
))

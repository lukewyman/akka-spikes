lazy val `akka-spikes` = project.in(file("."))
  .aggregate(`http-client`,  routing)

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

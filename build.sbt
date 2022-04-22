ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "ibm-mq"
  )

libraryDependencies += "org.typelevel"  %%    "cats-effect"     % "3.3.11"
libraryDependencies += "org.typelevel"  %%    "cats-core"       % "2.7.0"
libraryDependencies += "dev.fpinbo"     %%    "jms4s-ibm-mq"    % "0.1.4"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.5.1"
libraryDependencies += "com.typesafe" % "config" % "1.4.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11" % Runtime
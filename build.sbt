name := """flight-booking"""
organization := "com.soc"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.2"

libraryDependencies += guice

//JDBC Dependencies
libraryDependencies += javaJdbc
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.20.0"
libraryDependencies += "com.google.code.gson" % "gson" % "2.2.4"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.0"
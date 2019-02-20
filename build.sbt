name := """cs2340-team10-risk"""
version := "1.0"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.11.12", "2.12.7")

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test
libraryDependencies += caffeine
scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-Xfatal-warnings"
)

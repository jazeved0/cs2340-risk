name := """cs2340-team10-risk"""
version := "1.0"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.11.12", "2.12.7")

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb, SbtVuefy)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test
libraryDependencies += caffeine
scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-Xfatal-warnings"
)

Assets / VueKeys.vuefy / VueKeys.prodCommands := Set("stage")
Assets / VueKeys.vuefy / VueKeys.webpackBinary := {
  // Detect windows
  if (sys.props.getOrElse("os.name", "").toLowerCase.contains("win")) {
    (new File(".") / "node_modules" / ".bin" / "webpack.cmd").getAbsolutePath
  } else {
    (new File(".") / "node_modules" / ".bin" / "webpack").getAbsolutePath
  }
}
Assets / VueKeys.vuefy / VueKeys.webpackConfig := (new File(".") / "webpack.config.js").getAbsolutePath
// All non-entry-points components, which are not included directly in HTML, should have the prefix `_`.
// Webpack shouldn't compile non-entry-components directly. It's wasteful.
Assets / VueKeys.vuefy / excludeFilter := "_*"
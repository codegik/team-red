val scala3Version = "3.7.3"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "bitonic-scala3",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http" % "3.5.1",
      "dev.zio" %% "zio-redis" % "1.1.6",
      "dev.zio" %% "zio-schema-protobuf" % "1.7.5",
      "dev.zio" %% "zio-redis-embedded" % "1.1.6",
      "com.googlecode.xmemcached" % "xmemcached" % "2.4.8",
      "dev.zio" %% "zio-test-junit" % "2.1.21",
      "dev.zio" %% "zio-test" % "2.1.21" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.21" % Test,
      "dev.zio" %% "zio-http-testkit" % "3.5.1" % Test,
    )
  )

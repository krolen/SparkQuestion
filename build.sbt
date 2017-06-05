import Common.CommonDependencies._
import sbt.Keys.libraryDependencies

lazy val `SparkQuestion` = (project in file("."))
  .settings(Common.commonProjectSettings: _*)
  .settings(Common.commonAssemblySettings: _*)
  .settings(
    name := "SparkQuestion",
    version := "0.1",

    libraryDependencies ++= Seq(
      sparkCore,
      sparkSql,
      sparkStreaming,
      sparkKafka,

      typesafeConfig,
      libraryDependencies += "io.argonaut" %% "argonaut" % "6.1",
      "com.datadoghq" % "java-dogstatsd-client" % "2.3"
    ),

    assemblyMergeStrategy in assembly := {
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }

  )

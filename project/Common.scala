import sbt.Keys.{libraryDependencies, _}
import sbt._
import sbtassembly.AssemblyPlugin.autoImport.{assembly, assemblyJarName, assemblyOption}

object Common {
  private lazy val common = Seq(
    scalaVersion := "2.11.8",
    organization := "my.organization",

    resolvers += DefaultMavenRepository,
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= CommonDependencies.loggingLibs
  )

  lazy val commonProjectSettings: Seq[_root_.sbt.Def.Setting[_]] = common

  lazy val commonAssemblySettings = Seq(
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
    assemblyOption in assembly ~= {
      _.copy(includeScala = false)
    }
  )

  object CommonDependencies {
    lazy val sparkVersion = "2.1.0"
    lazy val typesafeConfigVersion = "1.3.1"
    lazy val kafkaVersion = "0.10.2.0"

    val sparkCore: ModuleID = "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
    val sparkSql: ModuleID = "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
    val sparkStreaming: ModuleID = "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided"
    val sparkKafka: ModuleID = "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion exclude("log4j", "log4j") exclude("org.spark-project.spark", "unused")

    val typesafeConfig: ModuleID = "com.typesafe" % "config" % typesafeConfigVersion

    val loggingLibs = Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "org.slf4j" % "slf4j-log4j12" % "1.7.25" exclude("log4j", "log4j"),
      "log4j" % "log4j" % "1.2.17" % "provided")
  }

}



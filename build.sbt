name := "datateam-challenge-master"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= {
  Seq(
    "org.slf4j" % "slf4j-api" % "1.7.5",
    "org.slf4j" % "slf4j-simple" % "1.7.5",
    "redis.clients" % "jedis" % "2.9.0",
    "com.typesafe" % "config" % "1.3.1",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "junit" % "junit" % "4.11" % "test"
  )
}

test in assembly := {}
mainClass in assembly := Some("com.agcat.airportstest.AirportMatcher")
assemblyJarName in assembly := "airporttest.jar"

val distZip = TaskKey[File]("distZip")

distZip := {
  val baseZipName = name.value + "-" + version.value

  val bins = ((baseDirectory.value / "src/main/shell") ** "*.sh").get
  val binPairs: Seq[(File, String)] = bins x Path.flatRebase(baseZipName + "/bin")

  val log4jFile = file("src/main/resources/log4j.properties") :: Nil
  val log4jPairs: Seq[(File, String)] = log4jFile x Path.flatRebase(baseZipName + "/conf")
  val appConfPair: (File, String) = (file("src/main/resources/reference.conf"), baseZipName + "/conf/application.conf")
  val confPairs: Seq[(File, String)] = log4jPairs :+ appConfPair

  val libFiles = file((assemblyOutputPath in assembly).value.toString) :: Nil
  val libPairs: Seq[(File, String)] = libFiles x Path.flatRebase(baseZipName + "/lib")

  val ooziePairs: Seq[(File, String)] = List(
    (file("oozie/workflow.xml"), baseZipName + "/oozie/workflow.xml")
    , (file("oozie/coordinator.xml"), baseZipName + "/oozie/coordinator.xml")
    , (file("oozie/workflow/conf.xml"), baseZipName + "/oozie/workflow/conf.xml")
    , (file("oozie/workflow/ifrs9import.xml"), baseZipName + "/oozie/workflow/ifrs9import.xml")
    , (file("oozie/workflow/save-operation.xml"), baseZipName + "/oozie/workflow/save-operation.xml")
    , (file("oozie/conf/job.properties"), baseZipName + "/oozie/conf/job.properties")
    , (file("oozie/conf/log4j.properties"), baseZipName + "/oozie/conf/log4j.properties")
  )

  val sources = binPairs ++ confPairs ++ libPairs ++ ooziePairs
  println(sources.mkString("\n"))
  val target: File = file("target/" + baseZipName + ".zip")
  println("--> " + target)
  IO.zip(sources.toIterable, target)
  target
}
distZip <<= distZip.dependsOn(assembly)
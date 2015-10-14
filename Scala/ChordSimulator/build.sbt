jarName in assembly := "project3.jar"

name := "ChordSimulator"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature")

resolvers ++= Seq("RoundEights" at "http://maven.spikemark.net/roundeights")

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.14",
    "com.typesafe.akka" %% "akka-remote" % "2.3.14",
    "com.roundeights" %% "hasher" % "1.2.0"
)








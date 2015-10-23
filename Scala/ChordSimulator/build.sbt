jarName in assembly := "project3.jar"

name := "ChordSimulator"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", 
					  "-deprecation")

javacOptions ++= Seq("-Xmx4g")

resolvers ++= Seq("RoundEights" at "http://maven.spikemark.net/roundeights")

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.0",
    "com.roundeights" %% "hasher" % "1.2.0"
)








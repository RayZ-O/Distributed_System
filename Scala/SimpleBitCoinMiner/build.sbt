name := "BitcointMiner"
     
version := "1.0"
     
scalaVersion := "2.11.7"

resolvers ++= Seq("RoundEights" at "http://maven.spikemark.net/roundeights")

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.13",
    "com.roundeights" %% "hasher" % "1.2.0"
)


  





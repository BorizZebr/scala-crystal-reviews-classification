
name := "scala-crystal-reviews-classification"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val `scala-crystal-reviews-classification` = (project in file("."))
  //.dependsOn(crystalDal)

//lazy val crystalDal = RootProject(uri("git://github.com/BorizZebr/scala-crystal-dal.git#master"))

resolvers ++= Seq(
  Resolver.bintrayRepo("akuznetsov", "russianmorphology")
)

libraryDependencies ++= Seq(
  // morphology
  "org.apache.lucene.morphology" % "russian" % "1.1",
  "org.apache.lucene.morphology" % "english" % "1.1",
  // html-parsing
  "org.jsoup" % "jsoup" % "1.9.2",
  "commons-io" % "commons-io" % "2.5",
  // spark
  "org.apache.spark" %% "spark-mllib" % "2.0.0",
  // nlp
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models",
  // database
  "com.typesafe.slick" %% "slick" % "3.1.1",
  //"mysql" % "mysql-connector-java" % "6.0.2",
  "com.h2database" % "h2" % "1.4.191",
  // test
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)
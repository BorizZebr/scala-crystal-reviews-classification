package com.zebrosoft

import org.apache.spark.SparkConf
import org.apache.spark.ml.feature.{StopWordsRemover, Word2Vec}
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.types.{ArrayType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SparkSession}

/**
  * Created by borisbondarenko on 30.08.16.
  */
object Word2VecPokeAround extends App {

  val alph = ('а' to 'я').toSet
  val sWords = StopWordsRemover.loadDefaultStopWords("russian")

  val conf = new SparkConf()
    .setMaster("local[2]")
    .setAppName("TestTestTest")

  val spark = SparkSession
    .builder
    .config(conf)
    .getOrCreate()

  val lines = spark.sparkContext.textFile("src/main/resources/input8.csv")
  val header = StructField("content", ArrayType(StringType, containsNull = true))
  val schema = StructType(Seq(header))
  val rows = lines
    .filter(_ != "content")
    .map(_.split("[ ,.!;:-]"))
    .map(words => words.filterNot(_.isEmpty))
    .map(words => words.map(_.toLowerCase))
    .map(words => words.map(_.filter(alph)))
    .map(words => words.filterNot(sWords.contains(_)))
    .map(words => words.filterNot(_.isEmpty))
    .map(a => Row(a))

  val documentDF = spark.createDataFrame(rows, schema)

  // Learn a mapping from words to Vectors.
  val word2Vec = new Word2Vec()
    .setInputCol("content")
    .setOutputCol("result")
    .setVectorSize(50)
    .setMinCount(0)
  val model = word2Vec.fit(documentDF)

  val result = model.transform(documentDF)
  result.collect().foreach { case Row(text: Seq[_], features: Vector) =>
    println(s"Text: [${text.mkString(", ")}] => \nVector: $features\n")
  }

  spark.stop()
}

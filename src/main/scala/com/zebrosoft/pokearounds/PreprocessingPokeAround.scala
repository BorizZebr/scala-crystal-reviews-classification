package com.zebrosoft.pokearounds

import java.io.{File, PrintWriter}

import com.zebrosoft.spellchecker.NorvigSpellChecker
import org.apache.spark.SparkConf
import org.apache.spark.ml.feature.StopWordsRemover
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{ArrayType, StringType, StructField, StructType}

import scala.io

/**
  * Created by bbondarenko on 9/4/2016 AD.
  */
object PreprocessingPokeAround extends App {

  // construct SpellChecker
  val bufferedSource = io.Source.fromURL(getClass.getResource("/dict.csv"))
  val dict = bufferedSource
    .getLines
    .map(_.split("\t"))
    .map(l => l(0) -> l(1).toDouble)
    .toMap
  bufferedSource.close
  val checker = new NorvigSpellChecker(dict)
  val alph = checker.alphabet

  // stop words
  val sWords = StopWordsRemover.loadDefaultStopWords("russian")

  // Spark config
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
  val preprocessedRow = lines
    .filter(_ != "content")
    .map(_.split(" "))
    .map(words => words.map(_.toLowerCase))
    .map(words => words.map(_.filter(alph)))
    .map(words => words.filterNot(sWords.contains(_)))
    .map(words => words.filterNot(_.isEmpty))
    .map(words => words.map(w => checker(w).getOrElse(w)))
    .collect()
    .map(words => words.mkString(" "))

  val pw = new PrintWriter(new File("src/main/resources/output8.csv"))
  pw.write("Content\n")
  preprocessedRow.foreach(s => pw.write(s"$s\n"))
  pw.close()

  spark.stop()
}

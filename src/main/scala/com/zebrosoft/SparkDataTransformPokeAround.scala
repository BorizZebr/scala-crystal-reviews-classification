package com.zebrosoft

import org.apache.spark.SparkConf
import org.apache.spark.ml.feature.StopWordsRemover
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SparkSession}
import java.io._

/**
  * Created by bbondarenko on 9/4/2016 AD.
  *
  */
object SparkDataTransformPokeAround extends App {

  val conf = new SparkConf()
    .setMaster("local[2]")
    .setAppName("TestTestTest")

  val spark = SparkSession
    .builder
    .config(conf)
    .getOrCreate()

  // сырой RDD частотного словаря
  val dictRDD = spark.sparkContext.textFile("src/main/resources/freqrnc2011.csv")

  // схема данных зашита в шапке файла
  val schemaString = "Lemma\tPoS\tFreq(ipm)\tR\tD\tDoc"

  // генерируем схему данных
  val fields = Array(
    StructField("Lemma", StringType),
    StructField("PoS", StringType),
    StructField("Freq(ipm)", DoubleType),
    StructField("R", IntegerType),
    StructField("D", IntegerType),
    StructField("Doc", IntegerType)
  )
  val schema = StructType(fields)

  // конвертим записи словая в row
  val rowRDD = dictRDD
    .filter(_ != schemaString)
    .map(_.split("\t"))
    .map(a => Row(
      a(0).toLowerCase,
      a(1),
      a(2).toDouble,
      a(3).toInt,
      a(4).toInt,
      a(5).toInt)
    )

  // применяем схему данных на RDD
  val dictDF = spark.createDataFrame(rowRDD, schema)
  println(s"before stop-words filtering, count: ${dictDF.count()}")

  // список Русских стоп-слов
  val stopWords = StopWordsRemover.loadDefaultStopWords("russian")

  // фильтруем стоп-слова и суммируем частоту употребления по
  // разным частям речи, т.к. наш spell-checker в них не умеет
  // по результату имеем Map: слово --> частота использования
  val freqMap = dictDF.filter { row =>
    ! stopWords.contains(row.getString(0).toLowerCase)
  }.groupBy("Lemma", "Freq(ipm)")
  .sum()
  .collect()
  .map(r => r.getString(0) -> r.getDouble(1))
  .toMap

  // сохраняем Mаp в файл для дальнейшего использования
  val pw = new PrintWriter(new File("src/main/resources/dict.csv"))
  pw.write("Lemma\tFreq\n")
  freqMap.toSeq.sortBy(_._1).foreach { case(k, v) =>
    pw.write(s"$k\t$v\n")
  }
  pw.close()

  // стопим Spark, больше не нужен
  spark.stop()
}

package com.zebrosoft

import org.apache.log4j.{Level, LogManager}
import org.apache.spark.SparkConf
import org.apache.spark.ml.feature.{StopWordsRemover, Word2Vec}
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.sql.types.{ArrayType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics

/**
  * Created by borisbondarenko on 30.08.16.
  */
object Word2VecPokeAround extends App {

  val idxToModel: Map[Int, String] = Map(
    0 -> "speed",
    1 -> "quality",
    2 -> "price",
    3 -> "gift",
    4 -> "service",
    5 -> "pack"
  )

  val alph = ('а' to 'я').toSet
  val sWords = StopWordsRemover.loadDefaultStopWords("russian")

  val conf = new SparkConf()
    .setMaster("local[2]")
    .setAppName("TestTestTest")

  val spark = SparkSession
    .builder
    .config(conf)
    .getOrCreate()

  LogManager.getRootLogger.setLevel(Level.WARN)

  val lines = spark.sparkContext.textFile("src/main/resources/ignored/input8.csv")
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

  val word2Vec = new Word2Vec()
    .setInputCol("content")
    .setOutputCol("result")
    .setVectorSize(50)
    .setMinCount(0)
  val wordsModel = word2Vec.fit(documentDF)

  val wordsVecRDD = wordsModel.transform(documentDF).rdd.map(_.getAs[DenseVector](1))
  val classesRDD = spark.sparkContext.textFile("src/main/resources/ignored/classifier.csv")
    .map(_.split(";").map(_.toInt))

  val wordsVecRddForJoin = wordsVecRDD.zipWithIndex().map { case(w, idx) => (idx, w) }
  val classesRddForJoin = classesRDD.zipWithIndex().map { case(c, idx) => (idx, c) }

  val unifiedData = (wordsVecRddForJoin join classesRddForJoin).flatMap { case(_, (words, classes)) =>
    classes.zipWithIndex.map { case(c, idx) =>
      idx -> LabeledPoint(c, Vectors.dense(words.toArray))
    }
  }

  val splitData = unifiedData.keys.distinct.collect.map { k =>
    k -> unifiedData.filter(_._1 == k).values
  }.toMap

  val models = splitData.map { case(idx, data) =>

    val splits = data.randomSplit(Array(0.6, 0.4), seed = 100500L)
    val training = splits(0).cache()
    val test = splits(1)

    val model = new LogisticRegressionWithLBFGS().run(training)

    val predictionAndLabels = test.map { case LabeledPoint(label, features) =>
      val prediction = model.predict(features)
      (prediction, label)
    }

    val metrics = new BinaryClassificationMetrics(predictionAndLabels)

    println(s"METRICS FOR MODEL: ${idxToModel(idx)}")

    // Precision by threshold
    val precision = metrics.precisionByThreshold
    precision.foreach { case (t, p) =>
      println(s"Threshold: $t, Precision: $p")
    }

    // Recall by threshold
    val recall = metrics.recallByThreshold
    recall.foreach { case (t, r) =>
      println(s"Threshold: $t, Recall: $r")
    }

    // Precision-Recall Curve
    val PRC = metrics.pr

    // F-measure
    val f1Score = metrics.fMeasureByThreshold
    f1Score.foreach { case (t, f) =>
      println(s"Threshold: $t, F-score: $f, Beta = 1")
    }

    val beta = 0.5
    val fScore = metrics.fMeasureByThreshold(beta)
    f1Score.foreach { case (t, f) =>
      println(s"Threshold: $t, F-score: $f, Beta = 0.5")
    }

    // AUPRC
    val auPRC = metrics.areaUnderPR
    println("Area under precision-recall curve = " + auPRC)

    // Compute thresholds used in ROC and PR curves
    val thresholds = precision.map(_._1)

    // ROC Curve
    val roc = metrics.roc

    // AUROC
    val auROC = metrics.areaUnderROC
    println("Area under ROC = " + auROC)

    model
  }

  spark.stop()
}

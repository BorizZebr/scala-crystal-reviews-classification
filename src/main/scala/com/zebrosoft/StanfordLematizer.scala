package com.zebrosoft

import edu.stanford.nlp.ling.CoreAnnotations.{LemmaAnnotation, SentencesAnnotation, TokensAnnotation}
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.PropertiesUtils

import scala.collection.JavaConversions._


/**
  * Created by borisbondarenko on 12.09.16.
  */
object StanfordLematizer {

  //val props: Properties = new Properties()
  //props.put("annotators", "tokenize, ssplit, pos, lemma")

  //val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

  val pipeline: StanfordCoreNLP = new StanfordCoreNLP(
    PropertiesUtils.asProperties(
      "annotators", "tokenize,ssplit,pos,lemma,parse,natlog",
      "ssplit.isOneSentence", "true"))

  def apply(documentText: String): Seq[String] = {
    val document = new Annotation(documentText)
    // run all Annotators on this text
    this.pipeline.annotate(document)
    for {
      sentence <- document.get(classOf[SentencesAnnotation])
      token <- sentence.get(classOf[TokensAnnotation])
    } yield token.get(classOf[LemmaAnnotation])
  }
}
package com.zebrosoft

import org.apache.lucene.morphology.russian.RussianLuceneMorphology


/**
  * Created by borisbondarenko on 19.09.16.
  */
object LuceneLematizer {

  import scala.collection.JavaConversions._

  var morph = new RussianLuceneMorphology()

  def apply(word: String): String = morph.getNormalForms(word).head
}

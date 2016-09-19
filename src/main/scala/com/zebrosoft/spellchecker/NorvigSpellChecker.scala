package com.zebrosoft.spellchecker

/**
  * Created by bbondarenko on 9/4/2016 AD.
  *
  * By the paper of Peter Norvig: http://norvig.com/spell-correct.html
  * Based on implementation of Pathikrit Bhowmick: https://gist.github.com/pathikrit/d5b26fe1c166a97e2162
  */
class NorvigSpellChecker(dict: Map[String, Double], level: Int = 2) {

  val freq: Map[String, Double] = dict withDefaultValue 0

  // collect all possible symbols
  val alphabet: Set[Char] = freq.keys.flatMap(_.toSeq).toSet

  def edit(n: Int)(word: String): Set[String] = n match {
    case 0 => Set(word)
    case 1 =>
      val chars = word.toSeq
      val splits = chars.indices map chars.splitAt

      val deletes = splits collect {case (a, b0 +: b1) => a ++ b1}
      val transposes = splits collect {case (a, b0 +: b1 +: b2) => a ++: b1 +: b0 +: b2}
      val replaces = alphabet flatMap {c => splits collect {case (a, b0 +: b1) => a ++: c +: b1}}
      val inserts = alphabet flatMap {c => splits map {case (a, b) => a ++: c +: b}}

      (deletes ++ transposes ++ replaces ++ inserts).map(_.mkString).toSet
    case _ => edit(n - 1)(word) flatMap edit(1)
  }

  def apply(word: String): Option[String] =
    Stream.tabulate(level + 1)(i => edit(i)(word) maxBy freq).find(freq.contains)
}

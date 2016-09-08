package com.zebrosoft

import java.io.{File, PrintWriter}

import org.apache.commons.io.FilenameUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.collection.JavaConversions._
import scala.io.Source

/**
  * Created by bbondarenko on 9/4/2016 AD.
  */
object ParseHtmlDictionary extends App {

  val path = getClass.getResource("/dict")
  val folder = new File(path.getPath)
  val dictionary = folder.listFiles
    .toList
    .filter(file =>
      FilenameUtils.getExtension(file.getPath) == "html")
      //&& FilenameUtils.getName(file.getPath) == "bae.html")
    .map { file =>
      println(s"reading ${file.getName}")
      Source.fromFile(
        file.getPath,
        "Windows-1251").mkString
    }
    .zipWithIndex
    .map { case(lines, idx) =>
      println(s"parsing HTML $idx")
      (Jsoup.parse(lines), idx)
    }
    .map { case(page, idx) =>
      println(s"parsing words $idx")
      parsePage(page)
    }
    .foldLeft(Map.empty[String, Double])(_ ++ _)

  val pw = new PrintWriter(new File("src/main/resources/parsed-html.csv"))
  dictionary.foreach {
    case(k, v) => pw.write(s"$k\t$v\n")
  }
  pw.close()


  def parsePage(html: Document): Map[String, Double] = {
    html
      .select(".morf")
      .select("tr")
      .foldLeft((Map.empty[String, Double], Double.MinPositiveValue)) { case((map, last), el) =>
        val children = el.select("td")
        children.size match {
          case 0 | 1 => (map, last)
          case _ =>
            val name = children.first.text.replace(" ", "")
            val aEls = el.select("a")
            aEls.size match {
              case 0 =>
                if (map.contains(name)) (map, last)
                else (map + (name -> last), last)
              case _ =>
                val freq =
                  if (
                    children.size < 4 ||
                    children.last.text.isEmpty ||
                    children.last.text.toDouble == 0
                  ) Double.MinPositiveValue
                  else children.last.text.toDouble
                (map + (name -> freq), freq)
            }
        }
      }._1
  }
}

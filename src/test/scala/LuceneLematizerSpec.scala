import com.zebrosoft.LuceneLematizer
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by borisbondarenko on 19.09.16.
  */
class LuceneLematizerSpec
  extends FlatSpec
    with Matchers {

  "Lematizer" should "lematize sentance" in {

    val res =
      """
        |Кандидат в президенты США Хиллари Клинтон после инцидента на траурной
        |церемонии в Нью-Йорке заявила журналистам, что чувствует себя прекрасно.
        |Представитель политика подтвердил, что Клинтон покинула акцию в память о
        |жертвах теракта 11 сентября до ее завершения.
      """.stripMargin
        .split("[ .,:;\t\n]")
        .map(_.toLowerCase)
        .map(_.replaceAll("[^а-яА-Я]", "").trim)
        .filterNot(_.isEmpty)
        .map(LuceneLematizer(_))
        .mkString(" ")

    println(res)
  }
}

import scala.concurrent.ExecutionContext.Implicits.global

object TestBingSearch extends App {

    val engine = new BingSearch("dZHx2LeVZb9BHorIDiwiIPTetFDaeqOgBHBJAScDzFE")
    val test = engine.search("'what is spelunking'").map(engine.parse)
        test.onSuccess{ case results =>
        {
            results.zipWithIndex.foreach { case (str, index) =>
                println(s"${index + 1} $str")
            }
        }
    }
}

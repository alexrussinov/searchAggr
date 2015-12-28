/**
 * Created by alex on 28.12.15.
 */
import scala.concurrent.ExecutionContext.Implicits.global
object TestGoogleSearch extends App {

    val engine = new GoogleSearch("AIzaSyDzkcGGhgkB4jjY22OavyJuyfuNf5AuMTg", "008366210748654328620:cicfenuzhr0")
    val result = engine.search("'what is spelunking'").map(engine.parse)
    result.onSuccess{ case r =>
        {
            r.zipWithIndex.foreach { case (str, index) =>
                println(s"${index + 1} $str")
            }
        }
    }
}

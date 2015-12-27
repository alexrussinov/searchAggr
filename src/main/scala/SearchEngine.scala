import play.api.libs.json.JsValue
import play.api.libs.ws.ning._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Aleksey Voronets on 27.12.15.
  */
trait SearchEngine {

    def baseUrl: String

    type AggregationStrategy = (Future[SearchResult]*) => Future[SearchResult]

    type SearchResult = JsValue

    type EstimateTotal = String

    type Client = { def close()}

    def client: Client

    def search(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[SearchResult]
}

case class GoogleSearch(apiKey: String, searchEngineId: String, baseUrl: String = "https://www.googleapis.com/customsearch/v1") extends SearchEngine {

    override def client() = { NingWSClient() }

    override def search(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[SearchResult] = {
        NingWSClient().url(baseUrl).withQueryString("key" -> apiKey, "cx" -> searchEngineId, "q" -> searchPhrase).get()
            .map(_.json)
    }
}


object TestGoogleSearch extends App {

    val engine = new GoogleSearch("AIzaSyDzkcGGhgkB4jjY22OavyJuyfuNf5AuMTg", "008366210748654328620:cicfenuzhr0")
    val test = engine.search("'what is spelunking'", Nil, f => ???)
    test.onSuccess{ case r =>
        {
            val total = (r \ "searchInformation" \ "totalResults").as[String]
            val results = (r \"items").as[Seq[JsValue]]
            println(s"Estimate total: ${total}")
            results.zipWithIndex.foreach { case (result, index) =>
                val url = (result \ "link").as[String]
                val title = (result \ "title").as[String]
                println(s"${index + 1} \n $title \n $url \n")
            }
            // TODO write automatic closer
            engine.client().close()
        }
    }
}
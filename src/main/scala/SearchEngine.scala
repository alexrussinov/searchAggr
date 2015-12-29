import play.api.libs.json.JsValue
import play.api.libs.ws.ning._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future}
import utils.CommonUtilsImplicits._


trait SearchTypes {
    type SearchResult = JsValue
    type AggregationStrategy = (Seq[SearchEngine], String) => Future[Seq[String]]
}

trait SearchEngine extends SearchTypes {

    type Client = { def close()}

    def client: Client

    def baseUrl: String

    def search(searchPhrase: String): Future[SearchResult]

    def parse(searchResult: SearchResult): Seq[String]
}

case class GoogleSearch(apiKey: String, searchEngineId: String, baseUrl: String = "https://www.googleapis.com/customsearch/v1") extends SearchEngine {

    override val client = NingWSClient()

    override def search(searchPhrase: String): Future[SearchResult] = {
        client.url(baseUrl).withQueryString("key" -> apiKey, "cx" -> searchEngineId, "q" -> searchPhrase).get()
            .map(_.json)
    }

    override def parse(searchResult: SearchResult) = {
        (searchResult \ "items").as[Seq[SearchResult]].map{ result =>
            val url = (result \ "link").as[String]
            val title = (result \ "title").as[String]
            s"$title \n $url"
        }
    }
}

case class BingSearch(apiKey: String, baseUrl: String = "https://api.datamarket.azure.com/Bing/Search/Web",
    top: Int = 5, skip: Int = 0) extends SearchEngine {

    override val client = NingWSClient()

    override def search(searchPhrase: String): Future[SearchResult] = {
        client.url(baseUrl).withHeaders("Authorization" -> ("Basic " + apiKey.toBingFormatKey))
            .withQueryString("$top" -> top.toString, "$skip" -> skip.toString,
                "$format" -> "JSON", "Query" -> searchPhrase).get().map(_.json)
    }

    override def parse(searchResult: SearchResult) = {
        (searchResult \"d" \ "results").as[Seq[SearchResult]].map{ result =>
            val url = (result \ "Url").as[String]
            val title = (result \ "Title").as[String]
            s"$title \n $url"
        }
    }
}





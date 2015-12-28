import java.util.Base64

import play.api.libs.json.JsValue
import play.api.libs.ws.ning._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, Future}
import utils.CommonUtilsImplicits._
import utils.IOUtils._

/**
  * Created by Aleksey Voronets on 27.12.15.
  */
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
//TODO extract code for closing to the helper method
case class GoogleSearch(apiKey: String, searchEngineId: String, baseUrl: String = "https://www.googleapis.com/customsearch/v1") extends SearchEngine {

    override def client() = { NingWSClient() }

    override def search(searchPhrase: String): Future[SearchResult] = {
        val promiseResult = Promise[SearchResult]()
        val result = client().url(baseUrl).withQueryString("key" -> apiKey, "cx" -> searchEngineId, "q" -> searchPhrase).get()
            .map(_.json)
        promiseResult.completeWith(result)
        result.onComplete{case _ => client().close()}
        promiseResult.future
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
    override def client() = { NingWSClient() }

    override def search(searchPhrase: String): Future[SearchResult] = {
        val promiseResult = Promise[SearchResult]()
        val result = client().url(baseUrl).withHeaders("Authorization" -> ("Basic " + apiKey.toBingFormatKey))
            .withQueryString("$top" -> top.toString, "$skip" -> skip.toString,
                "$format" -> "JSON", "Query" -> searchPhrase).get().map(_.json)
        result.onComplete{case _ => client().close()}
        promiseResult.future
    }

    override def parse(searchResult: SearchResult) = {
        (searchResult \"d" \ "results").as[Seq[SearchResult]].map{ result =>
            val url = (result \ "Url").as[String]
            val title = (result \ "Title").as[String]
            s"$title \n $url"
        }
    }
}





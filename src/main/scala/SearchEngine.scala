import java.util.Base64

import play.api.libs.json.JsValue
import play.api.libs.ws.ning._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import utils.CommonUtilsImplicits._

/**
  * Created by Aleksey Voronets on 27.12.15.
  */
trait SearchTypes {
    type SearchResult = JsValue
    type AggregationStrategy = (Future[SearchResult]*) => Future[SearchResult]
}

trait SearchEngine extends SearchTypes {

    def baseUrl: String

    type Client = { def close()}

    def client: Client

    def search(searchPhrase: String): Future[SearchResult]

    def parse(searchResult: SearchResult): Seq[String]
}

case class GoogleSearch(apiKey: String, searchEngineId: String, baseUrl: String = "https://www.googleapis.com/customsearch/v1") extends SearchEngine {

    override def client = { NingWSClient() }

    override def search(searchPhrase: String): Future[SearchResult] = {
        NingWSClient().url(baseUrl).withQueryString("key" -> apiKey, "cx" -> searchEngineId, "q" -> searchPhrase).get()
            .map(_.json)
    }

    override def parse(searchResult: SearchResult) = {
        (searchResult \ "items").as[Seq[SearchResult]].zipWithIndex.map{ case (result, index) =>
            val url = (result \ "link").as[String]
            val title = (result \ "title").as[String]
            s"${index + 1} \n $title \n $url \n"
        }
    }
}

case class BingSearch(apiKey: String, baseUrl: String = "https://api.datamarket.azure.com/Bing/Search/Web",
    top: Int = 5, skip: Int = 0) extends SearchEngine {

    override def client = { NingWSClient() }

    override def search(searchPhrase: String): Future[SearchResult] = {
        NingWSClient().url(baseUrl).withHeaders("Authorization" -> ("Basic " + apiKey.toBingFormatKey))
            .withQueryString("$top" -> top.toString, "$skip" -> skip.toString,
                "$format" -> "JSON", "Query" -> searchPhrase).get().map(_.json)
    }

    override def parse(searchResult: SearchResult) = {
        (searchResult \"d" \ "results").as[Seq[SearchResult]].zipWithIndex.map{ case (result, index) =>
            val url = (result \ "Url").as[String]
            val title = (result \ "Title").as[String]
            s"${index + 1} \n $title \n $url \n"
        }
    }
}


object TestGoogleSearch extends App {

    val engine = new GoogleSearch("AIzaSyDzkcGGhgkB4jjY22OavyJuyfuNf5AuMTg", "008366210748654328620:cicfenuzhr0")
    val test = engine.search("'what is spelunking'")
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
            engine.client.close()
        }
    }
}

object TestBingSearch extends App {

    val engine = new BingSearch("dZHx2LeVZb9BHorIDiwiIPTetFDaeqOgBHBJAScDzFE")
    val test = engine.search("'what is spelunking'")
        test.onSuccess{ case r =>
        {
            val results = (r \"d" \ "results").as[Seq[JsValue]]
            results.zipWithIndex.foreach { case (result, index) =>
                val url = (result \ "Url").as[String]
                val title = (result \ "Title").as[String]
                println(s"${index + 1} \n $title \n $url \n")
            }
            // TODO write automatic closer
            engine.client.close()
        }
    }
}
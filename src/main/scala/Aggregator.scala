import play.api.libs.iteratee.Enumerator
import utils.CommonUtilsImplicits.FutureExtended

import scala.concurrent.{Future}
import scala.util.{Try, Failure, Success}

/**
  * Created by Aleksey Voronets on 28.12.15.
  */
trait Aggregator extends SearchTypes {
    def parser()
    def aggregateSearch(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[Seq[Try[(SearchEngine, SearchResult)]]]
}

object Aggregator
{
    def roundRobin(searchEngines: (SearchEngine, SearchEngine), searchPhrase: String): Future[Seq[String]] = {
        val engine1 = searchEngines._1
        val engine2 = searchEngines._2
        val engine1Result = engine1.search(searchPhrase).map(result => engine1.parse(result))
        val engine2Result = engine2.search(searchPhrase).map(result => engine2.parse(result))
        for{
            result1 <- engine1Result
            result2 <- engine2Result
        } yield (result1.zip(result2).foldRight(Seq[String]())((a, acc) => (acc :+ a._1) :+ a._2))
    }

    def interleavingStrategy(searchEngines: Seq[SearchEngine], searchPhrase: String) = {
        val enumerators = searchEngines.map(_.search(searchPhrase)).map(futureResult => Enumerator.flatten(futureResult.map(Enumerator(_))))
        val s = Enumerator.interleave(enumerators).run()
    }
    def apply(): Aggregator = {
        new Aggregator {
            def parser() = ???
            def aggregateSearch(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[Seq[Try[(SearchEngine, SearchResult)]]] =
            {
                val searchResultsTry: Seq[Future[Try[(SearchEngine, SearchResult)]]] =
                    searchEngines.map{engine => engine.search(searchPhrase).toFutureTry.map{result => result.map((engine, _))}}
                Future.sequence(searchResultsTry).map(_.filter(_.isSuccess))
            }
        }
    }
}
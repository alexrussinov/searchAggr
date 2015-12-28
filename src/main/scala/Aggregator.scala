import play.api.libs.iteratee.{Iteratee, Enumerator}
import utils.CommonUtilsImplicits.FutureExtended

import scala.annotation.tailrec
import scala.concurrent.{Future}
import scala.util.{Try, Failure, Success}

/**
  * Created by Aleksey Voronets on 28.12.15.
  */
trait Aggregator extends SearchTypes {
    def aggregateSearch(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[Seq[Try[(SearchEngine, SearchResult)]]]
}

object Aggregator
{

    def mergeResults[T](results: Seq[Seq[T]]): Seq[T] = {
        @tailrec
        def step(results: Seq[Seq[T]], acc: Seq[T]): Seq[T] = {
            if (results.forall(_.isEmpty)) acc
            else step(results.withFilter(_.nonEmpty).map(_.tail), acc ++ results.flatMap(_.headOption))
        }
        step(results, Nil)
    }



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

    /** FIFO web service that responds first appear first in the result set **/
    def interleavingStrategy(searchEngines: Seq[SearchEngine], searchPhrase: String): Future[Seq[String]] = {
        val enumerators = searchEngines.map(engine => engine.search(searchPhrase).map(engine.parse))
            .map(futureResult => Enumerator.flatten(futureResult.map(Enumerator(_))))
        Enumerator.interleave(enumerators).run(Iteratee.fold(Seq[String]()){(acc, el) => acc ++ el})
    }

    def apply(): Aggregator = {
        new Aggregator {
            def aggregateSearch(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[Seq[Try[(SearchEngine, SearchResult)]]] =
            {
                val searchResultsTry: Seq[Future[Try[(SearchEngine, SearchResult)]]] =
                    searchEngines.map{engine => engine.search(searchPhrase).toFutureTry.map{result => result.map((engine, _))}}
                Future.sequence(searchResultsTry).map(_.filter(_.isSuccess))
            }
        }
    }
}
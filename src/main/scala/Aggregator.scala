import play.api.libs.iteratee.{Iteratee, Enumerator}
import utils.CommonUtilsImplicits.FutureExtended
import scala.annotation.tailrec
import scala.concurrent.{Future}
import scala.util.{Try, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Aleksey Voronets on 28.12.15.
  */
trait Aggregator extends SearchTypes {
    private def mergeResults[T](results: Seq[Seq[T]]): Seq[T] = {
        @tailrec
        def step(results: Seq[Seq[T]], acc: Seq[T]): Seq[T] = {
            if (results.forall(_.isEmpty)) acc
            else step(results.withFilter(_.nonEmpty).map(_.tail), acc ++ results.flatMap(_.headOption))
        }
        step(results, Nil)
    }

    private def executeQuery(searchEngines: Seq[SearchEngine], searchPhrase: String) = {
        searchEngines.map(engine => engine.search(searchPhrase).map(engine.parse))
    }

    def roundRobin1: AggregationStrategy =
        (searchEngines, searchPhrase) => {
            Future.sequence(executeQuery(searchEngines, searchPhrase)).map(mergeResults)
        }

    /** FIFO web service that responds first appear first in the result set **/
    def interleavingStrategy: AggregationStrategy = {
        (searchEngines, searchPhrase) => {
            val enumerators = executeQuery(searchEngines, searchPhrase).map(futureResult => Enumerator.flatten(futureResult.map(Enumerator(_))))
            Enumerator.interleave(enumerators).run(Iteratee.fold(Seq[String]()) { (acc, el) => acc ++ el })
        }
    }

    def aggregateSearch(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[Seq[String]]
}

object Strategies {

}


object Aggregator {

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

    def apply(): Aggregator = {
        new Aggregator {
            def aggregateSearch(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[Seq[String]] =
                aggregationStrategy(searchEngines, searchPhrase)
        }
    }
}
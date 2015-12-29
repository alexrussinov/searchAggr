import play.api.libs.iteratee.{Iteratee, Enumerator}
import scala.annotation.tailrec
import scala.concurrent.{Future}
import scala.concurrent.ExecutionContext.Implicits.global

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

    /**
      * When all futures are completed, combine the result in round robin fashion
      * @return
      */
    def roundRobin: AggregationStrategy =
        (searchEngines, searchPhrase) => {
            Future.sequence(executeQuery(searchEngines, searchPhrase)).map(mergeResults)
        }

    /**
      * FIFO - results from the future which is completed first, appears first in the aggregated set
      * @return
      */
    def fifoStrategy: AggregationStrategy = {
        (searchEngines, searchPhrase) => {
            val enumerators = executeQuery(searchEngines, searchPhrase).map(futureResult => Enumerator.flatten(futureResult.map(Enumerator(_))))
            Enumerator.interleave(enumerators).run(Iteratee.fold(Seq[String]()) { (acc, el) => acc ++ el })
        }
    }

    def aggregateSearch(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[Seq[String]]
}

object Aggregator {

    def apply(): Aggregator = {
        new Aggregator {
            def aggregateSearch(searchPhrase: String, searchEngines: Seq[SearchEngine], aggregationStrategy: AggregationStrategy): Future[Seq[String]] =
                aggregationStrategy(searchEngines, searchPhrase)
        }
    }
}
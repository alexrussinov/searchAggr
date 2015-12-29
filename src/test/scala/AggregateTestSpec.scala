import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span, Millis}
import play.api.libs.json.{JsNull}
import scala.concurrent.Future
import Matchers._
import scala.concurrent.ExecutionContext.Implicits.global


class AggregateTestSpec extends FunSuite with ScalaFutures{
    implicit val defaultPatience =
        PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

    test("Aggregate search with DummyEngines - round robin strategy"){
        val aggregator = Aggregator()
        val engines = Seq(new DummyEngine1Impl, new DummyEngine2Impl)
        val result = aggregator.aggregateSearch("test phrase", engines, aggregator.roundRobin)

        whenReady(result){ r =>
            r should contain inOrder("A", "D", "B", "E", "C", "F")
        }
    }

    test("Aggregate search with DummyEngines - fifo strategy"){
        val aggregator = Aggregator()
        val engines = Seq(new DummyEngine1Impl, new DummyEngine2Impl)
        val result = aggregator.aggregateSearch("test phrase", engines, aggregator.fifoStrategy)

        whenReady(result){ r =>
            r should contain inOrder("D", "E", "F", "A", "B", "C")
        }
    }
}

trait DummyEngine extends SearchEngine{

    override def client() = new { def close() = {} }

    override def baseUrl: String = "url"
}

class DummyEngine1Impl extends DummyEngine{

    override def search(searchPhrase: String): Future[SearchResult] = Future{ Thread.sleep(1000); JsNull }

    override def parse(searchResult: SearchResult): Seq[String] = Seq("A", "B", "C")
}

class DummyEngine2Impl extends DummyEngine{

    override def search(searchPhrase: String): Future[SearchResult] = Future{ Thread.sleep(500); JsNull }

    override def parse(searchResult: SearchResult): Seq[String] = Seq("D", "E", "F")
}

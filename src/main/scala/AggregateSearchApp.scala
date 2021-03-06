import scala.concurrent.ExecutionContext.Implicits.global

object AggregateSearchApp extends App {
    val aggregator = Aggregator()
    val engines = Seq(new BingSearch("dZHx2LeVZb9BHorIDiwiIPTetFDaeqOgBHBJAScDzFE"),
        new GoogleSearch("AIzaSyDzkcGGhgkB4jjY22OavyJuyfuNf5AuMTg", "008366210748654328620:cicfenuzhr0"))

    val roundRobinResult = aggregator.aggregateSearch("'what is spelunking'", engines, aggregator.roundRobin)
    val fifoResult = aggregator.aggregateSearch("'what is spelunking'", engines, aggregator.fifoStrategy)

    roundRobinResult.onSuccess{ case result =>
        println("Round robin results: ")
        result.foreach(print)
    }

    fifoResult.onSuccess{ case result =>
        println("Fifo results: ")
        result.foreach(print)
    }
}

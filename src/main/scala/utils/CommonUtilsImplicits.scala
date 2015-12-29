package utils

import javax.xml.bind.DatatypeConverter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Try, Failure, Success}

object CommonUtilsImplicits {
    implicit class BingFormatKey(key: String){
        def toBingFormatKey: String = {
            DatatypeConverter.printBase64Binary((key + ":" + key).getBytes("UTF-8"))
        }
    }

    implicit class FutureExtended[T](future: Future[T]){
        def toFutureTry: Future[Try[T]] = {
            future.map(Success(_)).recover({case ex => Failure(ex)})
        }
    }
}

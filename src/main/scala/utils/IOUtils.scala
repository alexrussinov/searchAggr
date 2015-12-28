package utils
import scala.concurrent.Future

/**
  * Created by Aleksey Voronets on 27.12.15.
  */
object IOUtils extends IOUtils
trait IOUtils {
    def ensureClose[T <: {def close()}, R](resource: T)(block: T => Future[R]) = try { block(resource) } finally { if(resource != null)resource.close()}
}
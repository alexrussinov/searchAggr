package utils

object IOUtils extends IOUtils
trait IOUtils {
    def ensureClose[T <: {def close()}, R](resource: T)(block: T => R) = try { block(resource) } finally { if(resource != null)resource.close()}
}
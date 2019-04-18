package controllers

import java.io.File
import common.Resources
import common.Resources.StatusCodes
import play.api.mvc.{Action, AnyContent, MessagesBaseController, MessagesControllerComponents}
import scala.concurrent.ExecutionContext

/**
  * Instantiated controller to handle file requests, child object of a
  * <code>MainController</code>
  */
class FileController(val controllerComponents: MessagesControllerComponents)
    extends MessagesBaseController {
  val Wildcard = "."

  /**
    * Builds a path by removing the first folder if it exists
    * @param root The base folder to replace the old one with (gets prepended)
    * @param base The original string to process
    * @return A modified filepath wrapped in a RelativeFile object
    */
  def formatFilepath(root: String)(implicit base: String): RelativeFile =
    RelativeFile(replaceRelativePath(root))

  /**
    * Replaces the root of a relative filepath with the new root, or replaces
    * the first folder it finds if no oldRoot is specified
    * @param newRoot the new root of the relative path
    * @param oldRoot the old root of the relative path, or "*" to match any single folder
    * @param path the path to process
    * @return the processed path
    */
  def replaceRelativePath(newRoot: String, oldRoot: String = "*")
                    (implicit path: String): String =
    newRoot + (oldRoot match {
      // Remove first directory regardless of what it is
      case "*" => path.indexOf('/') match {
        case -1 => path
        case i => path.substring(i)
      }
      // Remove old root
      case _ => path.replace(oldRoot, "")
    })


  /**
    * Builds a path for the docs file
    * @param root The base folder to replace the old one with (Resources.DocsPath)
    * @param base The original string to process
    * @return A heavily modified filepath wrapped in a RelativeFile object,
    *         or a UrlRedirect if a redirect is necessary
    */
  def formatDocsFilepath(root: String)(implicit base: String): InitialFileResponse = {
    val substr = replaceRelativePath("", Resources.DocsPath.init)
    // Redirect '/api' URLs to '/api/'
    if (substr == "/api") {
      UrlRedirect(Resources.DocsPath)
    } else {
      RelativeFile(root + (if (substr.indexOf('.') == -1) {
        substr + (if (substr.last == '/') "index.html" else ".html")
      } else {
        substr
      }))
    }
  }

  /** Represents the result of the initial file resolution stage */
  sealed trait InitialFileResponse
  /** Represents the result of the final file resolution stage */
  sealed trait FileResponse
  /** Represents a parsed and transformed relative filepath */
  case class RelativeFile(path: String) extends InitialFileResponse
  /** Represents a redirect response */
  case class UrlRedirect(to: String) extends FileResponse with InitialFileResponse
  /** Represents an error response */
  case class Error(message: String, code: Int = StatusCodes.NOT_FOUND)
    extends FileResponse with InitialFileResponse
  /** Represents a file that exists; wraps a file object */
  case class ResolvedFile(obj: File) extends FileResponse

  /**
    * Constructs a NotFound Error object for the path with a 404 status
    * @param path The path that was not found
    * @return An error object
    */
  def notFound(implicit path: String): Error =
    Error(s"Could not find $path", StatusCodes.NOT_FOUND)

  def specialDirectory(implicit path: String): InitialFileResponse =
    findByKey(Resources.DirectorySpecialMappings, path.startsWith) match {
      case Some(mapping) => formatFilepath(mapping)
      case None => notFound
    }

  def specialFile(implicit path: String): InitialFileResponse =
    findByKey(Resources.FileSpecialMappings, path == _) match {
      case Some(mapping) => RelativeFile(mapping)
      case None => notFound
    }

  def handleDocs(path: String): InitialFileResponse =
    if (Resources.DocsEnabled || !path.startsWith("docs/api")) {
      formatDocsFilepath(Resources.DocsRoot.init)(path)
    } else {
      Error("API docs are not enabled (try building for production)",
        StatusCodes.MOVED_PERMANENTLY)
    }

  def findByKey[K, V](map: Map[K, V], pred: K => Boolean): Option[V] = {
    map.keySet.find(pred) match {
      case Some(k) => Some(map(k))
      case None => None
    }
  }

  /**
    * Transforms a raw file path to the initial result of file resolution
    * @param path The raw file path included with the HTTP request
    * @return the result
    */
  def resolveFilepath(implicit path: String): InitialFileResponse = path match {
      case p if Resources.DirectorySpecialMappings.keySet.exists(path.startsWith) =>
        specialDirectory(p)
      case p if Resources.FileSpecialMappings.keySet.contains(path) =>
        specialFile(p)
      case p if p.startsWith(Resources.DocsPath.init) =>
        handleDocs(p)
      case p if Resources.DirectorySpecialMappings.contains(Wildcard) =>
        RelativeFile(s"${Resources.DirectorySpecialMappings(Wildcard)}/$p")
      case _ =>
        Error("Wildcard reroute not set. Preventing access to root directory",
          StatusCodes.UNAUTHORIZED)
  }

  /**
    * Transforms a raw file path to the final result of a file resolution
    * by chaining a call with <code>resolveFilePath(...)</code>
    * @param path The raw file path included with the HTTP request
    * @return the result
    */
  def resolveFile(implicit path: String): FileResponse = {
    resolveFilepath(path) match {
      case e: Error => e
      case r: UrlRedirect => r
      case RelativeFile(filename) =>
        val file = new File(filename)
        if (file.exists) ResolvedFile(file) else notFound
    }
  }

  /**
    * Router method to route file requests (default) to their proper targets
    * @param path The raw file path included with the HTTP request
    * @return either an error page in HTML, a redirect to another resource,
    *         or the actual file (if it was successfully resolved)
    */
  def route(path: String)(implicit ec: ExecutionContext): Action[AnyContent] =
    Action {
      resolveFile(path) match {
        case Error(m, c) => ErrorHandler.renderErrorPage(c, m)
        case ResolvedFile(file) => Ok.sendFile(file)
        case UrlRedirect(to) => Redirect(to)
      }
    }
}

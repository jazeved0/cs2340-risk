package views

import play.twirl.api.Html

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.matching.Regex

object DocsFormat {
  val TerminalMap: mutable.Map[String, String] = mutable.LinkedHashMap(
    """&lbrk;\S*[\n\r]+""" -> "<br/>",
    "&prmt; " -> "<span class=\"prompt\"></span>",
    "&wrmt; " -> "<span class=\"win-prompt\"></span>"
  )
  val LeadingSpaceRegex: Regex = """^[\r\n]+([ ]*)""".r
  val NewlineRegex: String = """[\r\n]+"""
  val Newline: String = "\n"

  def terminal(text: String, highlight: Boolean = true): Html =
    Html(s"""<pre class="terminal"><code${if (highlight) """ class="bash"""" else ""}>${applyMap(removeLeadingIndent(text), TerminalMap)}</code></pre>""")

  def link(url: String, text: String): Html =
    Html(s"""<a href="$url" target="_blank" rel="noopener">$text</a>""")

  def note(content: Html): Html = {
    Html(s"""<div class="note"><p><strong>Note: </strong>${content.toString()}</p></div>""")
  }

  def removeLeadingIndent(source: String): String = {
    val spaces: String = LeadingSpaceRegex.findAllIn(source).matchData.collectFirst { case i => i }
      .fold("")(m => m.group(1).toString)
    q"""$source""".map(l => l.replaceFirst(s"($spaces)", "")).mkString(Newline)
  }

  @tailrec
  def applyMap(source: String, map: mutable.Map[String, String]): String = map match {
    case _ if map.isEmpty => source
    case _ => applyMap(source.replaceAll(map.head._1, map.head._2), map.tail)
  }

  implicit class QHelper(val sc : StringContext) {
    def q(args : Any*): Seq[String] = {
      val strings = sc.parts.iterator
      val expressions = args.iterator
      val buf = new StringBuffer(strings.next)
      while(strings.hasNext) {
        buf append expressions.next
        buf append strings.next
      }
      buf.toString.split(NewlineRegex)
        .toSeq
        .filter(_ != "")
    }
  }
}

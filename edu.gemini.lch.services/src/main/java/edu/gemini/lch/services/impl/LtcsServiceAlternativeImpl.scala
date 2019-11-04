package edu.gemini.lch.services.impl

import edu.gemini.lch.services.{ConfigurationService, LtcsService}
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import org.apache.http.client.methods.HttpGet
import org.springframework.stereotype.Service
import javax.annotation.{PostConstruct, PreDestroy, Resource}

import scala.xml.{NodeSeq, XML}
import org.springframework.scheduling.annotation.Scheduled
import org.apache.log4j.Logger

import scala.collection.JavaConversions._
import edu.gemini.lch.services.LtcsService.Collision
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalQuery

import edu.gemini.lch.configuration.Configuration

/**
 * Experimental implementation of an alternative LTCS service implementation.
 * For reasons that escape my understanding it seems that the web service uses a "prediction" mode which returns
 * different results from the "actual" predictions and collisions listed on the summaries page. Since the operators
 * use the summaries page as their reference it seemed to be the easiest way to do some HTML scraping and get
 * the collisions from the summary HTML page instead of trying to talk the web service into returning identical
 * values to what the summaries page is giving us.
 */
@Service
class LtcsServiceAlternativeImpl extends LtcsService {

  private val logger = Logger.getLogger(classOf[LtcsServiceAlternativeImpl])

  @Resource private var configurationService: ConfigurationService = null

  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss MMM dd yyyy 'HST'")
  private val httpClient = new DefaultHttpClient
  private val httpHandler = new BasicResponseHandler

  private var currentStatus: LtcsService.Snapshot = new SnapshotImpl {
    val getMessage = "Not yet connected."
    val getCollisions = new java.util.ArrayList[LtcsService.Collision]()
    val getError = LtcsService.Error.NOT_CONNECTED
  }

  /**
   * Destroys the connection manager.
   * Called by Spring when shutting down the application.
   */
  @PreDestroy private def destroy() {
    httpClient.getConnectionManager.shutdown
  }

  /**
   * Gets the most current snapshot.
   **/
  @Override def getSnapshot: LtcsService.Snapshot =
    currentStatus

  /**
   * Updates the snapshot in regular intervals.
   */
  @Scheduled(fixedDelay = 10000)
  def update() {
    logger.debug("updating ltcs snapshot")
    try {
      val ltcsHost = configurationService getString Configuration.Value.LTCS_URL

      val summaryGet = new HttpGet(ltcsHost + "/ltcs/screens/status_and_alarm.php")
      val summaryPage = httpClient.execute(summaryGet, httpHandler)
      if (summaryPage contains ("LTCS PROCESSES ARE DOWN")) {
        currentStatus = new SnapshotImpl {
          val getMessage = "LTCS processes are down."
          val getCollisions = new java.util.ArrayList[LtcsService.Collision]()
          val getError = LtcsService.Error.PROCESSES_DOWN
        }

      } else {

        val detailsGet = new HttpGet(ltcsHost + "/ltcs/screens/predict_detail.php?laser=GEMINI")
        val detailsPage = httpClient.execute(detailsGet, httpHandler)
        val allCollisionsSorted: Seq[Collision] =
          (collisions(summaryPage) ++ predictions(detailsPage) ++ previews(summaryPage)) sortBy (_.getStart.toInstant.toEpochMilli)

        allCollisionsSorted.foreach(c => logger.info("-->" + c.getObservatory + " " + c.getStart))

        currentStatus = new SnapshotImpl {
          val getMessage = "LTCS up and running."
          val getCollisions = new java.util.ArrayList[LtcsService.Collision](allCollisionsSorted)
          val getError = LtcsService.Error.NONE
        }

      }

    } catch {

      case e: IOException => {
          logger.error("Could not connect to LTCS", e)
          currentStatus = new SnapshotImpl {
            val getMessage = "Error connecting to LTCS: " + e.getMessage
            val getCollisions = new java.util.ArrayList[LtcsService.Collision]()
            val getError = LtcsService.Error.NOT_CONNECTED
        }
      }
      case e: Exception => {
        logger.error("Could not access LTCS", e)
        currentStatus = new SnapshotImpl {
          val getMessage = "Error accessing LTCS: " + e.getMessage
          val getCollisions = new java.util.ArrayList[LtcsService.Collision]()
          val getError = LtcsService.Error.OTHER
        }
      }

    }
  }

  /**
   * Parses the HTML details page by scraping all predicted collisions from the page.
   * @return
   */
  def predictions(page: String): Seq[LtcsService.Collision] = {
    val xml = XML loadString fixPage(page)
    val tables = xml \\ "html" \\ "body" \\ "table"
    tables map parsePredictionTable
  }

  /**
   * Parses the HTML summary page and scrapes all collisions from the page.
   * @return
   */
  def collisions(page: String): Seq[LtcsService.Collision] = {
    val fixedPage = fixPage(page)
    val xml = XML loadString fixedPage
    val mainTable = xml \\ "html" \\ "body" \\ "table"
    val collisionsTable = (mainTable \ "tr") (3) \\ "td" \\ "table"
    parseSummaryTable(collisionsTable)
  }

  /**
   * Parses the HTML summary page and scrapes all previews from the page.
   * @return
   */
  def previews(page: String): Seq[LtcsService.Collision] = {
    val fixedPage = fixPage(page)
    val xml = XML loadString fixedPage
    val mainTable = xml \\ "html" \\ "body" \\ "table"
    val predictionsTable = (mainTable \ "tr") (5) \\ "td" \\ "table"
    parseSummaryTable(predictionsTable)
  }

  /**
   * Translate summary page table into collisions.
   */
  private def parseSummaryTable(table: NodeSeq): Seq[LtcsService.Collision] = {
    // take all rows, drop first one (header line) and get rid of collisions that don't involve Gemini as the lasing telescope
    val rows = table \\ "tr" drop 1 filter (n => (n\\"td")(0).text.contains("GEMINI"))
    // Recycling the parse functionality from the original service since the collision tables contain
    // exactly the same information as the web service returns.
    LtcsServiceImpl parseCollisions(
      (rows map (row => {
        row \\ "td" map (col => {                 // take all cols of each row
          col.text.trim                           // take trimmed text of all columns
        }) mkString " "                           // combine values of columns to one string
      })) mkString " ",                           // combine all rows to one string
      ZonedDateTime.now()
      )
  }

  /**
   * Parses a HTML table describing a collision.
   * @param table
   * @return
   */
  private def parsePredictionTable(table: NodeSeq): LtcsService.Collision = {
    val valuesMap = namedValues((table \\ "tr"))
    collisionFromNamedValues(valuesMap)
  }

  /**
   * Extracts a map with named values from the rows of a collision table.
   * The first column is used as the name, the second column is the value.
   */
  private def namedValues(rows: NodeSeq): Map[String, String] =
    rows map (r => {
      val cols = r \\ "td"
      (cols(0).text.trim, (cols(1).text.trim))
    }) toMap

  /**
   * Creates a collision from the named value map we scraped from a HTML collision table.
   */
  private def collisionFromNamedValues(values: Map[String, String]): LtcsService.Collision =
    new CollisionImpl {
      val getObservatory: String = values("Involved Telescope")
      val getStart: ZonedDateTime = ZonedDateTime.parse(values("Start Time"), timeFormatter)
      val getEnd: ZonedDateTime = ZonedDateTime.parse(values("End Time"), timeFormatter)
      val getPriority: String = if (values("Laser Has Priority") equalsIgnoreCase "NO") getObservatory else "GEMINI"
    }

    /**
     * Fixes broken html from LTCS server so that SAX parser accepts it.
     */
  private def fixPage(page: String) = {
      val meta = "<(meta|META) .*>".r             // meta tags without end tag
      val input = "<input .*>".r                  // input tags without end tag
      val script = "<script>.*</script>".r        // any script code
      val bgcolor = "bgcolor=#*[0-9A-Fa-f]{6}".r  // bgcolor attributes without value in double quotes
      val page2 = meta.replaceAllIn(page, "")
      val page3 = input.replaceAllIn(page2, "")
      val page4 = script.replaceAllIn(page3, "")
      val page5 = bgcolor.replaceAllIn(page4, "")
      page5.
        replace("\\\"", "\"").
        replace(" & ", " &amp; ").
        replace("<br>", "").
        replace("</br>", "").
        replace("<b>", "").
        replace("</b>", "").
        replace("<i>", "").
        replace("</i>", "").
        replace("<hr>", "").
        replace("</hr>", "").
        replace("<tbody>", "").
        replace("</tbody>", "")
    }

  /**
   * Implementation class for LtcsService.Snapshot interface.
   */
  private abstract class SnapshotImpl extends LtcsService.Snapshot {
    override def isConnected = getError == LtcsService.Error.NONE
  }

  /**
   * Implementation class for LtcsService.Collision interface.
   */
  private abstract class CollisionImpl extends LtcsService.Collision {
    override def geminiHasPriority = if (getPriority equalsIgnoreCase "GEMINI") true else false
    override def contains(time: ZonedDateTime) = false
    override def compareTo(other: LtcsService.Collision) = getStart compareTo getEnd
  }

}

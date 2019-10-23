package edu.gemini.lch.services.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import javax.annotation.Resource
import edu.gemini.lch.services.LtcsService
import scala.collection.JavaConversions._
import org.springframework.http.HttpStatus
import edu.gemini.lch.services.model.LtcsCollision
import org.joda.time.format.DateTimeFormat
import java.util
import javax.servlet.http.HttpServletResponse

/**
 * A controller class for the LTCS web services.
 */

@Controller
@RequestMapping(value=Array("/ltcs"))
class LtcsController {

  @Resource
  var ltcsService: LtcsService = null

  @RequestMapping(value=Array("/collisions"), method=Array(RequestMethod.GET), produces=Array("application/xml", "application/json"))
  @ResponseBody
  def queryCollisions(response: HttpServletResponse) : LtcsCollision.List = {
    val status = ltcsService.getSnapshot
    status getError match {
      case LtcsService.Error.NONE => {
        val collisions = status.getCollisions map {c => new LtcsCollision(c.getObservatory, c.getPriority, c.getStart.toDate, c.getEnd.toDate)}
        new LtcsCollision.List(collisions)
      }
      case LtcsService.Error.PROCESSES_DOWN => {
        response sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), status.getMessage)
        null
      }
      case _ => {
        response sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), status.getMessage)
        null
      }
    }
  }

  // ==== TEST ONLY ====
  @RequestMapping(value=Array("/collisions/test"), method=Array(RequestMethod.GET), produces=Array("application/xml", "application/json"))
  @ResponseBody
  def queryCollisionsTest() : LtcsCollision.List = {
    val t = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZoneUTC()
    val l = new util.ArrayList[LtcsCollision]()
    l.add(new LtcsCollision("UH2.2", "UH2.2", t.parseDateTime("2013-01-05 06:00:00").toDate, t.parseDateTime("2013-01-05 06:05:00").toDate))
    l.add(new LtcsCollision("KECK1", "GEMINI", t.parseDateTime("2013-01-05 12:00:00").toDate, t.parseDateTime("2013-01-05 12:05:00").toDate))
    new LtcsCollision.List(l)
  }

  // -- error handling
  @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE, reason="The LTCS system processes are down.")
  class ServiceUnavailableException extends RuntimeException

  @ResponseStatus( value = HttpStatus.INTERNAL_SERVER_ERROR)
  class InternalErrorException extends RuntimeException


}



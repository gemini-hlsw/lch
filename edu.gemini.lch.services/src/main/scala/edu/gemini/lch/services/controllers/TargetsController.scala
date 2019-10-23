package edu.gemini.lch.services.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import javax.annotation.Resource
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import scala.collection.JavaConversions._
import org.springframework.http.HttpStatus
import edu.gemini.lch.services.{AlarmService, LaserTargetsService, LaserNightService}
import edu.gemini.lch.services.model.{LaserTarget => XmlLaserTarget, ShortObservation => ShortObservationTO, ClearanceWindow => ClearanceWindowTO, ShutteringWindow => ShutteringWindowTO, Coordinates}

// aliases
import ShortObservationTO.{List => ObservationListTO}

/**
 * A controller class for the LTTS web services.
 */

@Controller
@RequestMapping(value=Array("/nights/{nightId}/targets"))
class TargetsController {

  val formatter = DateTimeFormat.forPattern("yyyy/MM/dd").withZone(DateTimeZone.UTC)

  @Resource
  var laserNightService: LaserNightService = null

  @Resource
  var laserTargetsService: LaserTargetsService = null

  @Resource
  var alarmService: AlarmService = null

  @RequestMapping(value=Array("", "/"), method=Array(RequestMethod.GET), produces=Array("application/xml", "application/json"))
  @ResponseBody
  def queryTarget(
      @PathVariable nightId: java.lang.Long,
      @RequestParam(required=false) raDeg:  java.lang.Double,
      @RequestParam(required=false) decDeg: java.lang.Double,
      @RequestParam(required=false) azDeg:  java.lang.Double,
      @RequestParam(required=false) elDeg:  java.lang.Double,
      @RequestParam(required=false) maxDistance: java.lang.Double,
      @RequestParam(required=false) includeBlanketClosures: java.lang.Boolean) : XmlLaserTarget = {

    val snapshot = alarmService.getSnapshot
    if (snapshot.getNight == null) throw new NightNotFound
    if (snapshot.getTarget == null) throw new NoClosestFoundException
    getTarget(snapshot, includeBlanketClosures)
  }

  @RequestMapping(value=Array("/{targetId}"), method=Array(RequestMethod.GET), produces=Array("application/xml", "application/json"))
  @ResponseBody
  def getTarget(
       @PathVariable nightId: Long,
       @PathVariable targetId: Long,
       @RequestParam(required=false) includeBlanketClosures: java.lang.Boolean) : XmlLaserTarget = {

    val snapshot = alarmService.getSnapshot
    if (snapshot.getNight == null) throw new NightNotFound
    if (snapshot.getTarget == null) throw new NoClosestFoundException
    getTarget(snapshot, includeBlanketClosures)

  }

  def getTarget(snapshot: AlarmService.Snapshot, includeBlanketClosures: java.lang.Boolean): XmlLaserTarget = {
      if (includeBlanketClosures != null && includeBlanketClosures) {
        TOFactory.toLaserTargetTO(snapshot.getNight, snapshot.getTarget, snapshot.getPropagationWindows.toList, snapshot.getShutteringWindows.toList)
      } else {
        TOFactory.toLaserTargetTO(snapshot.getNight, snapshot.getTarget)
      }
  }

  @RequestMapping(value=Array("/{targetId}/timeline"), method=Array(RequestMethod.GET), produces=Array("image/png"))
  @ResponseBody
  def getImageForTarget(
     @PathVariable nightId: Long,
     @PathVariable targetId : Long,
     @RequestParam width: Int,
     @RequestParam height: Int,
     @RequestParam now: Long,
     @RequestParam start: Long,
     @RequestParam end: Long,
     @RequestParam zone: String

     ) : Array[Byte] = {

    val snapshot = alarmService.getSnapshot
    val night = snapshot.getNight
    val target = snapshot.getTarget
    val timeZone = if (zone.toUpperCase().equals("UTC")) DateTimeZone.UTC else night.getSite().getTimeZone()


    if (snapshot.getNight == null) throw new NightNotFound

    if (targetId == 0) {
      laserTargetsService.getImage(night,width, height, new DateTime(start), new DateTime(end), new DateTime(now), timeZone)
    } else {
      if (target == null) throw new TargetNotFound
      laserTargetsService.getImage(night, target, width, height, new DateTime(start), new DateTime(end), new DateTime(now), timeZone)
    }

  }

  @RequestMapping(value=Array("/{targetId}/observations"), method=Array(RequestMethod.GET), produces=Array("application/xml", "application/json"))
  @ResponseBody
  def getObservationsForTarget(@PathVariable targetId : Long) : ObservationListTO = {

    val snapshot = alarmService.getSnapshot
    new ObservationListTO(
      snapshot.getObservations.flatMap(o => o.getTargets.
        map(t => new ShortObservationTO(o.getObservationId, t.getName, t.getType, TOFactory.toCoordinatesTO(t)))).
        toList
    )
  }

  // -- error handling
  @ResponseStatus(value=HttpStatus.NOT_FOUND, reason="There is no laser target in the vicinity of this position.")
  class NoClosestFoundException extends RuntimeException

  @ResponseStatus(value=HttpStatus.NOT_FOUND, reason="No laser night with this id.")
  class NightNotFound extends RuntimeException

  @ResponseStatus(value=HttpStatus.NOT_FOUND, reason="No laser target with this id.")
  class TargetNotFound extends RuntimeException

  @ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Query paramteres are invalid.")
  class InvalidQuery extends RuntimeException
}



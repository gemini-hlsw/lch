package edu.gemini.lch.services.controllers

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import javax.annotation.Resource

import scala.collection.JavaConversions._
import org.springframework.http.HttpStatus
import edu.gemini.lch.services.{AlarmService, LaserNightService, LaserTargetsService}
import edu.gemini.lch.services.model.{Coordinates, ClearanceWindow => ClearanceWindowTO, LaserTarget => XmlLaserTarget, ShortObservation => ShortObservationTO, ShutteringWindow => ShutteringWindowTO}

// aliases
import ShortObservationTO.{List => ObservationListTO}

/**
 * A controller class for the LTTS web services.
 */

@Controller
@RequestMapping(value=Array("/nights/{nightId}/targets"))
class TargetsController {

  val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("UTC"))

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
    val zoneId = if (zone.toUpperCase().equals("UTC")) ZoneId.of("UTC") else night.getSite().getZoneId()


    if (snapshot.getNight == null) throw new NightNotFound

    if (targetId == 0) {
      val start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault())
      val end   = ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault())
      val now   = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
      laserTargetsService.getImage(night,width, height, start, end, now, zoneId)
    } else {
      if (target == null) throw new TargetNotFound
      laserTargetsService.getImage(night, target, width, height,
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()),
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()),
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zoneId), zoneId)
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



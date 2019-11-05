package edu.gemini.lch.services.controllers

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import javax.annotation.Resource

import scala.collection.JavaConversions._
import org.springframework.http.HttpStatus
import edu.gemini.lch.services.{LaserNightService, SiteService}
import edu.gemini.lch.model._
import edu.gemini.lch.services.impl.VisibilityCalculatorImpl
import org.apache.log4j.Logger

// aliases
import edu.gemini.lch.services.model.{
  NightFull => NightTO,
  Night,
  LaserTarget => LaserTargetTO,
  Observation => ObservationTO,
  ObservationTarget => ObservationTargetTO,
  NightShort => NightShortTO,
  BlanketClosure => BlanketClosureTO
}
import edu.gemini.lch.services.model.NightShort.{
  List => NightShortListTO
}
import edu.gemini.lch.services.model.BlanketClosure.{
  List => BlanketClosureListTO
}

/**
 * Controller for LTTS services regarding laser nights.
 */
@Controller
@RequestMapping(value=Array("/nights"))
class NightsController {

  val LOGGER = Logger.getLogger(classOf[NightsController])

  val formatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"))

  @Resource var nightService: LaserNightService = null
  @Resource var siteService: SiteService = null

  val longAgo = ZonedDateTime.now().minusYears(1).toLocalDate.atStartOfDay(ZoneId.systemDefault())
  val farAway = ZonedDateTime.now().plusYears(1).toLocalDate.atStartOfDay(ZoneId.systemDefault())

  @RequestMapping(value=Array("", "/"), method=Array(RequestMethod.GET), produces=Array("application/xml", "application/json"))
  @ResponseBody
  def queryNights(
            @RequestParam(required=false) date: String,
            @RequestParam(required=false) from: String,
            @RequestParam(required=false) to: String,
            @RequestParam(required=false) full: Boolean,
            @RequestParam(required=false) now: Boolean,
            @RequestParam(required=false) laserLimit: java.lang.Double
            ) = {
    LOGGER.debug("/nights?date="+date+"&from="+from+"&to="+to+"&full="+full+"now="+now+"&laserLimit="+laserLimit)
    (date, from, to, now, full) match {
      case (null, null, null, true,  false) => getNightNow()
      case (null, null, null, false, false) => getNights(longAgo,      farAway)
      case (null, from, null, false, false) => getNights(getDay(from), farAway)
      case (null, null, to,   false, false) => getNights(longAgo,      getDay(to).plusDays(1))
      case (null, from, to,   false, false) => getNights(getDay(from), getDay(to).plusDays(1))
      case (date, null, null, false, false) => getNight(getDay(date))
      case (date, null, null, false, true ) => getFullNight(getDay(date), Option[Double](laserLimit))     // TODO: fails if laserLimit is null
      case _ => throw new BadRequestException
    }
  }

  @RequestMapping(value=Array("/{nightId}"), method=Array(RequestMethod.GET), produces=Array("application/xml", "application/json"))
  @ResponseBody
  def getNight(@PathVariable nightId: Long) = {
    LOGGER.debug("/nights/"+nightId)
    val night = nightService.getShortLaserNight(nightId)
    if (night == null) throw new NightNotFoundException
    toShortNightTOs(night)
  }

  @RequestMapping(value=Array("/{nightId}/blanketClosures"), method=Array(RequestMethod.GET), produces=Array("application/xml", "application/json"))
  @ResponseBody
  def getBlanketClosures(@PathVariable nightId: Long) = {
    LOGGER.debug("/nights/"+nightId+"/blanketClosures")
    new BlanketClosureListTO(
      nightService.getBlanketClosures(nightId).
        map(b => new BlanketClosureTO(b.getStart.toInstant, b.getEnd.toInstant))
    )
  }

  def getNightNow() = {
    val night = nightService.getShortLaserNightCovering(ZonedDateTime.now())
    if (night == null) throw new NightNotFoundException
    toShortNightTOs(night)
  }

  def getNights(from: ZonedDateTime, to: ZonedDateTime) : NightShortListTO =
    new NightShortListTO(
      nightService.getShortLaserNights(from.toLocalDate.atStartOfDay().atZone(from.getZone), to.toLocalDate.atStartOfDay().atZone(to.getZone)))
        map(n => toShortNightTOs(n)).
        toList
    )

  def getNight(date: DateTime) : Night = {
    val night = nightService.getShortLaserNight(date)
    if (night == null) throw new NightNotFoundException
    toShortNightTOs(night)
  }

  def getFullNight(date: DateTime, laserLimit: Option[Double]) : Night = {

    val night = nightService.loadLaserNight(date.withTimeAtStartOfDay())
    if (night == null) throw new NightNotFoundException

    val calculator = new VisibilityCalculatorImpl(night.getSite(), night.getStart(), night.getEnd, new java.lang.Double(laserLimit.getOrElse(40.0)))
    val targetTOs = TOFactory.toLaserTargetTOs(night, calculator, night.getLaserTargets.toSet)
    val laserTargetTOsMap = targetTOs map { lt => (lt.getId, lt)} toMap
    val observationTOs = toObservationTOs(night.getObservations.toSet, laserTargetTOsMap)

    val nightTO = new NightTO
    nightTO.getLaserTargets.addAll(targetTOs.toList)
    nightTO.getObservations.addAll((observationTOs.toList))

    nightTO
  }

  def getDay(nightId: String): DateTime = {
    try {
      formatter.parseDateTime(nightId).withTimeAtStartOfDay()
    } catch {
      case e: IllegalArgumentException => throw new BadRequestException
    }
  }

  def toObservationTOs(observations: Set[Observation], xmlLaserTargetsMap: Map[String, LaserTargetTO]): Set[ObservationTO] =
    observations map {
      obs: Observation =>
        val observationTargets = toObservationTargetTOs(obs.getTargets.toSet, xmlLaserTargetsMap)
        new ObservationTO(obs.getObservationId, observationTargets.toList)
    }

  def toShortNightTOs(night: SimpleLaserNight) =
    new NightShortTO(
      night.getId,
      night.getSite.name,
      night.getStart.toDate,
      night.getEnd.toDate,
      night.getLatestPrmSent.toDate,
      night.getLatestPamReceived.toDate)

  def toObservationTargetTOs(targets: Set[ObservationTarget], xmlLaserTargetsMap: Map[String, LaserTargetTO]) =
    targets map {
      target: ObservationTarget =>
        new ObservationTargetTO(
          target.getName,
          target.getType,
          xmlLaserTargetsMap.get(target.getLaserTarget.getId.toString).get
        )
    }


  // -- error handling
  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason="The request is invalid.")
  class BadRequestException extends RuntimeException

  @ResponseStatus( value = HttpStatus.NOT_FOUND, reason="This date is not a laser night." )
  class NightNotFoundException extends RuntimeException


}



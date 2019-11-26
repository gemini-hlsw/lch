package edu.gemini.lch.services.util

import java.time.format.DateTimeFormatter
import java.time.{Duration, ZoneId, ZonedDateTime}

import collection.mutable
import java.util.regex.{Matcher, Pattern}

import edu.gemini.lch.model.{AzElLaserTarget, LaserNight, RaDecLaserTarget}
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import edu.gemini.lch.services.ConfigurationService
import javax.annotation.Resource
import edu.gemini.lch.configuration.Configuration
import org.apache.log4j.Logger


/**
 * A simple specific purpose template engine for LTTS.
 */
@Component
@Scope("prototype")
class TemplateEngine(maybeNight: Option[LaserNight], maybeDate: Option[ZonedDateTime]) {

  val LOGGER = Logger getLogger classOf[TemplateEngine]

  // -- set by Spring
  @Resource
  var configurationService: ConfigurationService = null

  // -- actual class
  private val PATTERN_STRING = "\\$\\{\\{([^}]+)\\}\\}"
  private val pattern =  Pattern.compile(PATTERN_STRING)
  private val replacements = mutable.Map.empty[String, Option[String] => String]

  def this()                  { this(None, None) }
  def this(night: LaserNight) { this(Some(night), None) }
  def this(date: ZonedDateTime)    { this(None, Some(date)) }

  // Construction code
  {
    maybeNight.map(setNight) // set night if one has been set in constructor
    maybeDate.map(setDate)   // set date if one has been set
  }

  /**
   * Adds a replacement that will replace all occurrences of <code>key</code> with <code>transformation</code>.
   */
  def addReplacement(key: String, transformation: String) =
    replacements += key -> {_: Option[String] => transformation }

  /**
   * Adds a replacement that will replace all occurrences of <code>key</code> with the result of
   * <code>transformation()</code>.
   */
  def addReplacement(key: String, transformation: Option[String] => String) =
    replacements += key -> transformation

  /**
   * Convenience method that returns the filled template defined by a configuration value.
   */
  def fillTemplate(configuration: Configuration.Value) : String =
    fillTemplate(configurationService.getString(configuration))

  /**
   * Fills the given template.
   */
  def fillTemplate(template: String) : String = {
    val sb = new StringBuffer()
    val m = pattern.matcher(template)
    while (m find) {
      val transformed = transformTemplate(sb, m.group(1))
      m.appendReplacement(sb, Matcher.quoteReplacement(transformed))
    }
    m.appendTail(sb)
    sb.toString
  }

  def setDate(date: ZonedDateTime) = {
    addReplacement("SEMESTER", transformSemester(_, date))
  }

  def setNight(night: LaserNight) = {
    val tz = night.getSite.getZoneId
    addReplacement("NOW",                 transformDate(_, ZonedDateTime.now, tz))
    addReplacement("NIGHT-START",         transformDate(_, night getStart, tz))
    addReplacement("NIGHT-END",           transformDate(_, night getEnd, tz))
    addReplacement("NIGHT-PRM-SENT",      transformDateNullSafe(_, night getLatestPrmSent, tz))
    addReplacement("NIGHT-PAM-RECEIVED",  transformDateNullSafe(_, night getLatestPamReceived, tz))
    addReplacement("NIGHT-DURATION",      transformDuration(_, new Duration(night getStart, night getEnd)))
    setDate(night getStart)
  }

  def setLaserTarget(target: RaDecLaserTarget) = {
    addReplacement("TARGET-RA-DEGREES",   transformDouble(_, target.getRaDegrees))
    addReplacement("TARGET-DEC-DEGREES",  transformDouble(_, target.getDecDegrees))
  }

  def setLaserTarget(target: AzElLaserTarget) = {
    addReplacement("TARGET-AZ-DEGREES",   transformDouble(_, target.getAzDegrees))
    addReplacement("TARGET-EL-DEGREES",   transformDouble(_, target.getElDegrees))
  }


  private def transformTemplate(sb: StringBuffer, replacement: String) = {
    try {
      replacement splitAt (replacement indexOf '_') match {
        case ("", key)      => getReplacement(key, replacement) (None)
        case (key, pattern) => getReplacement(key, replacement) (Some(pattern.drop(1)))
      }
    } catch {
      case e: Exception => errorString(replacement)
    }
  }

  private def transformSemester(template: Option[String], date: ZonedDateTime) = {
    val offset = Integer.parseInt(template.getOrElse("0"))
    LaserNight.getSemester(date, offset)
  }

  private def transformDouble(template: Option[String], d: Double): String =
    String.format(template.getOrElse("%f"), d.asInstanceOf[java.lang.Double])

  /**
   * Checks for <code>null</code> dates before handling them.
   */
  private def transformDateNullSafe(template: Option[String], date: ZonedDateTime, zoneId: ZoneId) =
    if (date == null) "NULL" else transformDate(template, date, zoneId)

  /**
   * Transforms a date according to the template.
   * If no template is given, the default string representation is printed.
   */
  private def transformDate(template: Option[String], date: ZonedDateTime, zoneId: ZoneId) = {
    template match {
      case Some(string) => string split ('_') match {
        case Array("LOCAL", pattern)  => DateTimeFormatter.ofPattern(pattern).format(date.withZoneSameLocal(zoneId))
        case Array(timeZone, pattern) => DateTimeFormatter.ofPattern(pattern).format(date.withZoneSameInstant(ZoneId.of(timeZone)))
        case Array(pattern)           => DateTimeFormatter.ofPattern(pattern).format(date)
        case _                        => errorString(string)
      }
      case _ => date.toString
    }
  }

  /**
   * Transforms a duration.
   * The template is currently ignored, the output is always formatted as HH:mm:ss.
   */
  private def transformDuration(template: Option[String], duration: Duration) = {
    // TODO-JODA
    periodFormatter.print(duration.toPeriod)
  }

  /**
   * Creates a period formatter.
   */
  private val periodFormatter =
  // TODO-JODA
    new PeriodFormatterBuilder().
      printZeroAlways.
      minimumPrintedDigits(2).
      appendHours.appendSeparator(":").
      appendMinutes.appendSeparator(":").
      appendSeconds.toFormatter

  /**
   * Creates a string representing a replacement that failed.
   */
  private def errorString(string: String) = {
    LOGGER error "Could not resolve template '" + string + "'"
    "??" + string + "??"
  }

  /**
   * Gets the currently defined replacement or an error string if no replacement for a key is defined.
   */
  private def getReplacement(key: String, replacement: String) =
    replacements getOrElse (key, {_: Option[String] => errorString(replacement)})

}
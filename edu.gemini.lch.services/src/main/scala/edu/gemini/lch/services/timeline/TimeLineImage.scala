package edu.gemini.lch.services.timeline

import edu.gemini.lch.model._
import edu.gemini.lch.services.ModelFactory

import scala.collection.JavaConversions._
import java.awt.image.BufferedImage
import java.awt._

import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream
import java.awt.font.FontRenderContext
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, Period, ZoneId, ZonedDateTime}
import java.util.Date

import jsky.coords.WorldCoords

import scala.Some
import scala.List

import jsky.plot.util.SkyCalc

/**
 * Helper class for drawing time lines.
 */
case class TimeLineImage(
    night: LaserNight,
    imageStart: ZonedDateTime,
    imageEnd: ZonedDateTime,
    width: Int = 800,
    imgHeight: Int = 10,
    target: Option[LaserTarget] = None,
    text: Option[(Int, ZoneId)] = None,
    now: Option[ZonedDateTime] = None,
    buffers: Option[(Period, Period)] = None,
    drawElevationLine: Boolean = false) {

  def this(night: LaserNight) =
    this(
      night,
      night.getStart.withMinute(0).withSecond(0).withNano(0),
      night.getEnd.plusHours(1).withMinute(0).withSecond(0).withNano(0))

  def withTarget(target: LaserTarget) =
    copy(target = Some(target))

  def withTimes(imageStart: ZonedDateTime, imageEnd: ZonedDateTime) =
    copy(imageStart = imageStart, imageEnd = imageEnd)

  def withDimensions(width: Int, height: Int) =
    copy(width = width, imgHeight = height)

  def withText(zone: ZoneId, fontSize: Int) =
    copy(text = Some((fontSize, zone)))

  def withNowMarker(now: ZonedDateTime) =
    copy(now = Some(now))

  def withBuffers(bufferBefore: Period, bufferAfter: Period) =
    copy(buffers = Some((bufferBefore, bufferAfter)))

  def withElevationLine() =
    copy(drawElevationLine = true)

  // do some simple and cheap up front calculations needed for drawing the image
  val fontHeight = if (text.isDefined) text.get._1 + 2 else 0
  val height = if (imgHeight - fontHeight > 0) imgHeight - fontHeight else 1
  val imageDurationInSeconds = Duration.between(imageStart, imageEnd).getSeconds
  val sp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(imageStart.toInstant.toEpochMilli - imageStart.toInstant.toEpochMilli % spacing), ZoneId.systemDefault())
  val spacers = calcSpacers(sp)
  val imagePixelsPerSecond = (width.toDouble - spacers.length*2) / imageDurationInSeconds.toDouble


  // all the heavy lifting is done only when we actually need to create the image
  def bytes = {
    val calc = ModelFactory.createSunCalculator(night)
    val sky  = ModelFactory.createSkyCalculator(night.getSite)

    val parts =
      List[Part]():+
      Part(PartType.Day,                   new Date(calc.getSunset.getTime - 24*3600*1000), calc.getSunset):+
      Part(PartType.CivilTwilight,         calc.getSunset, calc.getCivilTwilightStart):+
      Part(PartType.NauticalTwilight,      calc.getCivilTwilightStart, calc.getNauticalTwilightStart):+
      Part(PartType.AstronomicalTwilight,  calc.getNauticalTwilightStart, calc.getAstronomicalTwilightStart):+
      Part(PartType.Night,                 calc.getAstronomicalTwilightStart, calc.getAstronomicalTwilightEnd):+
      Part(PartType.AstronomicalTwilight,  calc.getAstronomicalTwilightEnd, calc.getNauticalTwilightEnd):+
      Part(PartType.NauticalTwilight,      calc.getNauticalTwilightEnd, calc.getCivilTwilightEnd):+
      Part(PartType.CivilTwilight,         calc.getCivilTwilightEnd, calc.getSunrise):+
      Part(PartType.Day,                   calc.getSunrise, new Date(calc.getSunrise.getTime + 24*3600*1000))

    // TODO : nicefy this
    val timeline =
    if (target.isDefined) {
      new Timeline(parts).
        add((target.get.getVisibility.getVisibleIntervalsDuring(night) map (i => Part(PartType.Visible, i.start, i.end))).toList).                //TODO: is there a nicer way to do this? why is toList necessary?
        add((target.get.getVisibility.getVisibleIntervalsAboveLimitDuring(night) map (i => Part(PartType.AboveLimit, i.start, i.end))).toList).
        addPropagationWindows(target.get.getPropagationWindows.toList).
        addShutteringWindows(target.get.getShutteringWindows.toList).
        addBlanketClosures(night.getClosures.toList)
    } else {
      new Timeline(parts).
        addBlanketClosures(night.getClosures.toList)
    }

    val image = new BufferedImage(width, imgHeight, BufferedImage.TYPE_INT_RGB)
    val drawable = image.createGraphics()

    // white background
    drawable.setColor(TimeLineImage.COLOR_BACKGROUND)
    drawable.fillRect(0, 0, width, imgHeight)

    // draw parts
    val visibleParts = timeline.allParts.filter(p => p.start.isBefore(imageEnd.toInstant) && p.end.isAfter(imageStart.toInstant))
    visibleParts.filter(p => !p.types.contains(PartType.Closed)) foreach (p => drawOpen(drawable, p))
    visibleParts.filter(p => p.types.contains(PartType.Closed))  foreach (p => drawClosed(drawable, p))

    // draw elevation plot for raDec
    if (target.isDefined && drawElevationLine) {
      target.get match {
        case t: RaDecLaserTarget => drawElevation(drawable, t, sky)
        case _ =>
      }
    }

    spacers foreach (t => drawHourLine(drawable, t))
    if (now.isDefined) drawNowMarker(drawable, now.get)
    if (text.isDefined) spacers map (t => drawHourText(drawable, t))


    val imagebuffer = new ByteArrayOutputStream()
    this.synchronized {
      /* Write the image to a buffer. */
      ImageIO.write(image, "png", imagebuffer)
      image.flush()
      imagebuffer.close()
    }

    imagebuffer.toByteArray
  }

  private def drawElevation(drawable: Graphics2D, target: LaserTarget, sky: SkyCalc) {
    val c = new WorldCoords(target.getDegrees1, target.getDegrees2)
    val x = timeToX(imageStart)
    val y = elevationToY(c, imageStart, height, sky)
    drawable.setColor(new Color(130,130,130))
    drawElevationLine(drawable, c, imageStart.plusMinutes(2), x, y, sky)
  }

  private def drawElevationLine(drawable: Graphics2D, c: WorldCoords, t: ZonedDateTime, lastX: Int, lastY: Int, sky: SkyCalc) {
    if (t.isBefore(imageEnd)) {
      val x = timeToX(t)
      val y = elevationToY(c, t, height, sky)
      if (lastY < height && y < height) {
        drawable.drawLine(lastX, lastY, x, y)
      }
      val secondsPer10Pixels = 10.0 / imagePixelsPerSecond
      drawElevationLine(drawable, c, t.plusSeconds(secondsPer10Pixels.toInt), x, y, sky)
    }
  }

  private def elevationToY(c: WorldCoords, t: ZonedDateTime, height: Int, sky: SkyCalc) = {
    // Skycalc
    sky.calculate(c, Date.from(t.toInstant))
    (height - sky.getAltitude / 90.0 * height).round.toInt
  }



  /**
   * Draws a box around the current time.
   * Don't draw a solid object in order not to hide the color below.
   * @param t
   */
  private def drawNowMarker(drawable: Graphics2D, t: ZonedDateTime) {
    val x = timeToX(t)
    val upper = new Polygon( Array[Int](x-5, x+5, x), Array[Int](0, 0, 10), 3)
    val lower = new Polygon( Array[Int](x-5, x+5, x), Array[Int](height, height, height-10), 3)

    drawable.setColor(Color.LIGHT_GRAY)
    drawable.drawRect(x-2, 0, 4, height)

    drawable.setColor(TimeLineImage.COLOR_NOW)
    drawable.drawRect(x-1, 0, 2, height)
    drawable.fillPolygon(upper)
    drawable.fillPolygon(lower)

    drawable.setColor(Color.DARK_GRAY)
    drawable.draw(upper)
    drawable.draw(lower)
  }

  private def drawHourLine(drawable: Graphics2D, t: ZonedDateTime) {
    if (t.getMinute == 0) {
      drawable.setColor(TimeLineImage.COLOR_HOUR_LINES)
    } else {
      drawable.setColor(TimeLineImage.COLOR_TIME_LINES)
    }
    drawable.fillRect(timeToX(t), 0, TimeLineImage.HrsIntervalWidth, height)
  }

  private def drawOpen(drawable: Graphics2D, part: Part) {
    drawable.setColor(color(part))
    fillRect(drawable, part.start, part.end, 1)
  }

  private def drawClosed(drawable: Graphics2D, part: Part) {
    val paint = drawable.getPaint
    drawable.setPaint(TimeLineImage.stripesPaint)
    if (buffers.isDefined) {
      fillRect(drawable, part.start minus buffers.get._1, part.end plus buffers.get._2, 6)
    } else {
      fillRect(drawable, part.start, part.end, 6)
    }
    drawable.setPaint(paint)

    // draw a rectangle do denote the actual shuttering window and differentiate it from
    // the areas that are just buffer times
    val x0 = timeToX(part.start)
    val x1 = timeToX(part.end)
    if (x1 - x0 >= 2) {
      drawable.setColor(TimeLineImage.COLOR_CLOSED)
      drawable.drawRect(x0, 0, x1 - x0 - 1, height - 1)
    }

  }

  private def fillRect(drawable: Graphics2D, t0: ZonedDateTime, t1: ZonedDateTime, minWidth: Int) {
    val x0 = timeToX(t0)
    val x1 = timeToX(t1)
    if (x1 - x0 < minWidth)
        drawable.fillRect((x0 + x1 - minWidth) / 2, 0, minWidth, height)
      else
        drawable.fillRect(x0, 0, x1 - x0, height)
  }

  private def drawHourText(drawable: Graphics2D, t: ZonedDateTime) {
    val x = timeToX(t)
    val font = new Font("Helvetica", Font.BOLD, text.get._1)
    val ctx = new FontRenderContext(null, true, true)
    val hour = DateTimeFormatter.ofPattern("HH:mm").format(t)
    val bounds = font.getStringBounds(hour, ctx)
    val x2 = (x + 2 - bounds.getWidth/2).toInt
    // only draw text if it fits on image
    if (x2 >= 0 && x2 + bounds.getWidth <= width) {
      drawable.setColor(TimeLineImage.COLOR_FONT)
      drawable.setFont(font)
      drawable.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      drawable.drawString(hour, x2, imgHeight - 2)
    }
  }

  private def spacing : Int = {
    val approxPixelsPerSecond = width.toDouble / imageDurationInSeconds.toDouble
    if (approxPixelsPerSecond * 60 * 1 >= 50) 1*60*1000
    else if (approxPixelsPerSecond * 60 * 5 >= 50) 5*60*1000
    else if (approxPixelsPerSecond * 60 * 10 >= 50) 10*60*1000
    else if (approxPixelsPerSecond * 60 * 15 >= 50) 15*60*1000
    else if (approxPixelsPerSecond * 60 * 30 >= 50) 30*60*1000
    else 60*60*1000
  }

  private def calcSpacers(t: ZonedDateTime): List[ZonedDateTime] =
    if (t.isAfter(imageEnd)) {
      List()
    } else {
      t :: calcSpacers(t.plus(spacing, ChronoUnit.MILLIS))
    }


  private def timeToX(t: ZonedDateTime) =
    if (t.isBefore(imageStart)) 0
    else if (t.isAfter(imageEnd)) width
    else {
      val x = (new Duration(imageStart, t).getSeconds * imagePixelsPerSecond round).toInt
      // add additional space for all spacers that are before time t
      x + (spacers.count(st => st.isBefore(t)) * TimeLineImage.HrsIntervalWidth)
    }

  private def color(p: Part) =
    if      (!p.types.contains(PartType.Open) && p.types.contains(PartType.AboveLimit))                 TimeLineImage.COLOR_VISIBLE
    else if (!p.types.contains(PartType.Open) && p.types.contains(PartType.Visible))                    TimeLineImage.COLOR_VISIBLE_BELOW
    else if (p.types.contains(PartType.Open)  && p.types.contains(PartType.AboveLimit))                 TimeLineImage.COLOR_OPEN
    else if (p.types.contains(PartType.Open)  && p.types.contains(PartType.Visible))                    TimeLineImage.COLOR_OPEN_BELOW
    else if (p.types.contains(PartType.Night))                TimeLineImage.COLOR_NIGHT
    else if (p.types.contains(PartType.Day))                  TimeLineImage.COLOR_DAY
    else if (p.types.contains(PartType.AstronomicalTwilight)) TimeLineImage.COLOR_AST_TWI
    else if (p.types.contains(PartType.NauticalTwilight))     TimeLineImage.COLOR_NAU_TWI
    else if (p.types.contains(PartType.CivilTwilight))        TimeLineImage.COLOR_CIV_TWI
    else                                                      TimeLineImage.COLOR_DAY
}

object TimeLineImage {
  final val HrsIntervalWidth     = 2

  final val COLOR_DAY            = new Color(206, 239, 252)
  final val COLOR_CIV_TWI        = new Color(160, 160, 160)
  final val COLOR_NAU_TWI        = new Color(128, 128, 128)
  final val COLOR_AST_TWI        = new Color( 96,  96,  96)
  final val COLOR_NIGHT          = new Color( 50,  50,  50)
  final val COLOR_VISIBLE        = new Color(255, 178, 102)
  final val COLOR_VISIBLE_BELOW  = new Color(250, 130,  50)
  final val COLOR_OPEN           = new Color(  0, 204,   0)
  final val COLOR_OPEN_BELOW     = new Color(250, 130,  50)
  final val COLOR_CLOSED         = new Color(204,   0,   0)
  final val COLOR_FONT           = new Color(0, 0, 0)
  final val COLOR_TIME_LINES     = new Color(180, 180, 180)
  final val COLOR_HOUR_LINES     = Color.WHITE
  final val COLOR_BACKGROUND     = Color.WHITE
  final val COLOR_NOW            = Color.YELLOW
  final val COLOR_BUFFERS        = new Color(255, 180, 180)

  final val stripes = ImageIO.read(getClass getResourceAsStream "stripes.png")
  final val stripesPaint = new TexturePaint(stripes, new Rectangle(0,0, stripes.getWidth, stripes.getHeight()))
}


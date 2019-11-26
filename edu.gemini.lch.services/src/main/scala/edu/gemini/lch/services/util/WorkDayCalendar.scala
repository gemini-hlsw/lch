package edu.gemini.lch.services.util

import java.time.{DayOfWeek, Duration, ZoneId, ZonedDateTime}
import java.util.Date

import edu.gemini.lch.model.Holiday
import org.hibernate.SessionFactory
import javax.annotation.Resource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Helper object to calculate the number of working days (i.e. days excluding Saturdays, Sundays and holidays)
 * between two dates.
 */
@Component
@Scope("prototype")
class WorkDayCalendar {

  @Resource
  var sessionFactory : SessionFactory = null

  /**
   * Gets the number of standard days between now and the given point in time.
   */
  def daysBefore(end: ZonedDateTime): Long =
    daysBetween(ZonedDateTime.now, end)

  /**
   * Gets the number of standard days between the given points in time.
   */
  def daysBetween(start: ZonedDateTime, end: ZonedDateTime): Long =
    (Duration.between(start, end)).toDays

  /**
   * Gets the number of working days between now and the given point in time.
   */
  def workDaysBefore(end: ZonedDateTime) : Long =
    workDaysBetween(ZonedDateTime.now, end)

  /**
   * Gets the number of working days between the given points in time.
   * Working days are all Mondays to Fridays on which no holidays is observed.
   */
  def workDaysBetween(start: ZonedDateTime, end: ZonedDateTime) : Long = {
    val days = daysBetween(start, end)      // gets number of standard days
    val weekendDays = weekends(start, end)  // subtract weekend days (Sat/Sun), leaves us with all Mon, Tue, .. Fridays
    val holidayDays = holidays(start, end)  // subtract holidays (observed dates always fall on Mon - Fri)
    scala.math.max(0, days - weekendDays - holidayDays)
  }

  private def weekends(start: ZonedDateTime, end: ZonedDateTime) : Long =
    getDays(start, end).count(d => d.getDayOfWeek.compareTo(DayOfWeek.SATURDAY) >= 0)

  private def holidays(start: ZonedDateTime, end: ZonedDateTime) : Long = {
    // Hibernate
    val q = sessionFactory.getCurrentSession.getNamedQuery(Holiday.QUERY_FIND_BETWEEN_DATES)
    q.setTimestamp("first", Date.from(start.toInstant))
    q.setTimestamp("last", Date.from(end.toInstant))
    q.list.asInstanceOf[java.util.ArrayList[Holiday]].size()
  }

  /**
   * Gets a list with all days from <code>start</code> up to and excluding <code>end</code>.
   */
  def getDays(start: ZonedDateTime, end: ZonedDateTime) : List[ZonedDateTime] =
    if (start isBefore end) {
      List(start) ::: getDays(start.plusDays(1).toLocalDate.atStartOfDay(ZoneId.systemDefault()), end)
    } else {
      List.empty
    }

}

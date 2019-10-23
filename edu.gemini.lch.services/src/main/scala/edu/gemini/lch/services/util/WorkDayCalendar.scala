package edu.gemini.lch.services.util

import org.joda.time.{Duration, DateTimeConstants, DateTime}
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
   * @param end
   * @return
   */
  def daysBefore(end: DateTime): Long =
    daysBetween(DateTime.now, end)

  /**
   * Gets the number of standard days between the given points in time.
   * @param end
   * @return
   */
  def daysBetween(start: DateTime, end: DateTime): Long =
    (new Duration(start, end)).getStandardDays()

  /**
   * Gets the number of working days between now and the given point in time.
   * @param end
   * @return
   */
  def workDaysBefore(end: DateTime) : Long =
    workDaysBetween(DateTime.now, end)

  /**
   * Gets the number of working days between the given points in time.
   * Working days are all Mondays to Fridays on which no holidays is observed.
   * @param start
   * @param end
   * @return
   */
  def workDaysBetween(start: DateTime, end: DateTime) : Long = {
    val days = daysBetween(start, end)      // gets number of standard days
    val weekendDays = weekends(start, end)  // subtract weekend days (Sat/Sun), leaves us with all Mon, Tue, .. Fridays
    val holidayDays = holidays(start, end)  // subtract holidays (observed dates always fall on Mon - Fri)
    scala.math.max(0, days - weekendDays - holidayDays)
  }

  private def weekends(start: DateTime, end: DateTime) : Long =
    getDays(start, end).count(d => d.getDayOfWeek >= DateTimeConstants.SATURDAY)

  private def holidays(start: DateTime, end: DateTime) : Long = {
    val q = sessionFactory.getCurrentSession.getNamedQuery(Holiday.QUERY_FIND_BETWEEN_DATES)
    q.setTimestamp("first", start.toDate)
    q.setTimestamp("last", end.toDate)
    q.list.asInstanceOf[java.util.ArrayList[Holiday]].size()
  }

  /**
   * Gets a list with all days from <code>start</code> up to and excluding <code>end</code>.
   * @param start
   * @param end
   * @return
   */
  def getDays(start: DateTime, end: DateTime) : List[DateTime] =
    start isBefore end match {
      case true => List(start) ::: getDays(start.plusDays(1).withTimeAtStartOfDay(), end)
      case false => List.empty
    }

}

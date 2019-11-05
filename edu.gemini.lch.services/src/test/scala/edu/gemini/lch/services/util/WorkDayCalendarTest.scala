package edu.gemini.lch.services.util

import java.time.{ZoneId, ZonedDateTime}

import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Assert._
import org.junit.Test
import javax.annotation.Resource
import org.springframework.transaction.annotation.Transactional
import edu.gemini.lch.model.Holiday


/**
 * Check calculation of working days between two dates, i.e. the number fo days between two dates except for Saturdays
 * and Sundays and except for federal holidays. Federal holidays are read from the database, tests use 2012 holidays.
 */
@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("/spring-services-test-context.xml"))
class WorkDayCalendarTest {

  @Resource
  var calendar: WorkDayCalendar = null

  // create some test data
  private val thisMonday = ZonedDateTime.of(2012,12,17,0,0,0,0,ZoneId.systemDefault()) // Dec 17 2012 is a Monday
  private val thisTueday = ZonedDateTime.of(2012,12,18,0,0,0,0,ZoneId.systemDefault()) // Dec 18 2012 is a Tuesday
  private val thisThuday = ZonedDateTime.of(2012,12,20,0,0,0,0,ZoneId.systemDefault())
  private val thisFriday = ZonedDateTime.of(2012,12,21,0,0,0,0,ZoneId.systemDefault())
  private val thisSatday = ZonedDateTime.of(2012,12,22,0,0,0,0,ZoneId.systemDefault())
  private val thisSunday = ZonedDateTime.of(2012,12,23,0,0,0,0,ZoneId.systemDefault())
  private val nextMonday = ZonedDateTime.of(2012,12,24,0,0,0,0,ZoneId.systemDefault()) // Dec 24 2012 is a Monday
  private val nextTueday = ZonedDateTime.of(2012,12,25,0,0,0,0,ZoneId.systemDefault()) // Dec 25 2012 is a Tuesday and a Holiday
  private val nextFriday = ZonedDateTime.of(2012,12,28,0,0,0,0,ZoneId.systemDefault())

  @Test
  def doesGetDays : Unit = {
    assertEquals( 0, calendar.getDays(thisMonday, thisMonday).length)
    assertEquals( 1, calendar.getDays(thisMonday, thisTueday).length)
    assertEquals( 7, calendar.getDays(thisMonday, nextMonday).length)
  }

  // check Mondays to Fridays are returned
  @Test
  @Transactional(readOnly = true)
  def doesReturnWeekdays : Unit = {
    assertEquals( 0, calendar.workDaysBetween(thisMonday, thisMonday)) // {}
    assertEquals( 1, calendar.workDaysBetween(thisMonday, thisTueday)) // {Mon}
    assertEquals( 4, calendar.workDaysBetween(thisMonday, thisFriday)) // {Mon,Tue,Wed,Thu}
    assertEquals( 5, calendar.workDaysBetween(thisMonday, thisSatday)) // {Mon,Tue,Wed,Thu,Fri}
  }



  // check if Saturdays and Sundays are filtered properly
  @Test
  @Transactional(readOnly = true)
  def doesNotReturnWeekendDays : Unit = {
    assertEquals( 0, calendar.workDaysBetween(thisSatday, thisSunday)) // {}
    assertEquals( 5, calendar.workDaysBetween(thisMonday, thisSunday)) // {Mon,Tue,Wed,Thu,Fri}
    assertEquals( 5, calendar.workDaysBetween(thisMonday, nextMonday)) // {Mon,Tue,Wed,Thu,Fri}
    assertEquals( 6, calendar.workDaysBetween(thisMonday, nextTueday)) // {Mon,Tue,Wed,Thu,Fri,Mon}
  }

  // check if holidays are filtered
  @Test
  @Transactional(readOnly = true)
  def doesNotReturnHolidays : Unit = {
    assertEquals( 3, calendar.workDaysBetween(nextMonday, nextFriday)) // {Mon,Wed,Thu}
  }

  // check a combination of weekend and holidays from database (Thanksgiving on Thursday, Nov 22)
  @Test
  @Transactional(readOnly = true)
  def doesItAll : Unit = {
    assertEquals(3, calendar.workDaysBetween(ZonedDateTime.of(2012,11,21,0,0,0,0,ZoneId.systemDefault()), ZonedDateTime.of(2012,11,27,0,0,0,0,ZoneId.systemDefault()))) // {Wed, Fri, Mon}
  }

  // check a combination of weekend and holidays from database (Thanksgiving on Thursday, Nov 22)
  // set times not to be 00:00:00 but sometime during the day
  @Test
  @Transactional(readOnly = true)
  def doesItAllWithTimes : Unit = {
    assertEquals(3, calendar.workDaysBetween(ZonedDateTime.of(2012,11,21,9,0,0,0,ZoneId.systemDefault()), ZonedDateTime.of(2012,11,27,18,0,0,0,ZoneId.systemDefault()))) // {Wed, Fri, Mon}
  }

}

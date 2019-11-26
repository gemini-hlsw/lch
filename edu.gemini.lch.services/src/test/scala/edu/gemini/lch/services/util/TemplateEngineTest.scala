package edu.gemini.lch.services.util

import java.time.{ZoneId, ZonedDateTime}

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.junit.{Assert, Test}
import edu.gemini.lch.model.{AzElLaserTarget, LaserNight, Site}
import org.springframework.test.context.ContextConfiguration
import edu.gemini.lch.services.impl.Factory
import javax.annotation.Resource
import edu.gemini.lch.configuration.Configuration
import edu.gemini.lch.services.LaserNightService
import edu.gemini.odb.browser.{OdbBrowser, QueryResult}

/**
 * Test the template engine.
 */
@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("/spring-services-test-context.xml"))
class TemplateEngineTest {

  @Resource
  var laserNightService: LaserNightService = null
  @Resource
  var odbBrowser: OdbBrowser = null
  @Resource
  var factory: Factory = null

  val nightStart = ZonedDateTime.of(2012,12,20,18,0,0,0, ZoneId.of("HST"))
  val nightEnd   = ZonedDateTime.of(2012,12,21,6,0,0,0,ZoneId.of("HST"))
  val night = new LaserNight(Site.NORTH, nightStart, nightEnd)

  @Test
  def doesFillTemplates: Unit = {
    val engine = factory.createTemplateEngine(night)
    Assert assertEquals("20121220", engine.fillTemplate("${{NIGHT-START_HST_yyyyMMdd}}"))
    Assert assertEquals("06:00:00", engine.fillTemplate("${{NIGHT-END_HST_hh:mm:ss}}"))
    Assert assertEquals("lalala lululu 20121220 lailailai", engine.fillTemplate("lalala lululu ${{NIGHT-START_HST_yyyyMMdd}} lailailai"))
    Assert assertEquals("\n\n12:00:00\n\n", engine.fillTemplate("\n\n${{NIGHT-DURATION}}\n\n"))
  }

  @Test
  def canAddReplacements: Unit = {
    val engine = factory.createTemplateEngine(night)
    engine addReplacement("REPLACE-THIS", "WITH THAT")
    Assert assertEquals("WITH THAT", engine.fillTemplate("${{REPLACE-THIS}}"));
    engine addReplacement("REPLACE-THIS", "WITH OTHER THAT")
    Assert assertEquals("WITH OTHER THAT", engine.fillTemplate("${{REPLACE-THIS}}"));
  }

  @Test
  def doesHandleErrors: Unit = {
    val engine = factory createTemplateEngine night
    Assert assertEquals("??BROKEN??", engine.fillTemplate("${{BROKEN}}"))
    Assert assertEquals("??BROKEN_broken??", engine.fillTemplate("${{BROKEN_broken}}"))
    Assert assertEquals("??NIGHT-END_broken??", engine.fillTemplate("${{NIGHT-END_broken}}"))
    Assert assertEquals("??NIGHT-END_HST_broken??", engine.fillTemplate("${{NIGHT-END_HST_broken}}"))
  }

  @Test
  def canProduceLaserTargets: Unit = {
    val engine = factory createTemplateEngine night
    engine.setLaserTarget(new AzElLaserTarget(null, 10.0d, 20.0d))
    Assert.assertEquals("10.000000", engine.fillTemplate("${{TARGET-AZ-DEGREES}}"))
    Assert.assertEquals("20.000000", engine.fillTemplate("${{TARGET-EL-DEGREES}}"))
    Assert.assertEquals("10.000", engine.fillTemplate("${{TARGET-AZ-DEGREES_%.3f}}"))
    Assert.assertEquals("20.000", engine.fillTemplate("${{TARGET-EL-DEGREES_%.3f}}"))
  }

  @Test
  def canProduceSemesters: Unit = {
    val engine = factory createTemplateEngine night
    Assert assertEquals("2012B", engine.fillTemplate("${{SEMESTER}}"))
    Assert assertEquals("2012B", engine.fillTemplate("${{SEMESTER_0}}"))
    Assert assertEquals("2012B", engine.fillTemplate("${{SEMESTER_-0}}"))
    Assert assertEquals("2013A", engine.fillTemplate("${{SEMESTER_1}}"))
    Assert assertEquals("2012A", engine.fillTemplate("${{SEMESTER_-1}}"))
  }

  // check if Spring wiring works
  @Test
  def canFillTemplateFromDB: Unit = {
    val engine = factory createTemplateEngine night
    engine fillTemplate Configuration.Value.EMAILS_NEW_TARGETS_EMAIL_SUBJECT_TEMPLATE
  }

  @Test
  def canUseEngineCreatingPrmFiles: Unit = {
    val queryResult = odbBrowser query "queryResult2012B.xml"
    val day = ZonedDateTime.of(2012, 11, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
    val night = laserNightService createAndPopulateLaserNight(day, queryResult, new QueryResult())
    val files = laserNightService createRaDecPrmFiles night

    // check that we get a result
    Assert.assertEquals(1, files size)
    val filledTemplate = files.iterator.next.getFile

    // check for some strings we know should appear in the filled template
    // NOTE: when changing the template or the data these values will have to be changed, too
    Assert assertTrue filledTemplate.contains("Start Date/Time (UTC):        2012 Nov 02 (307) 03:47:05")
    Assert assertTrue filledTemplate.contains("End Date/Time (UTC):          2012 Nov 02 (307) 16:24:08")
    Assert assertTrue filledTemplate.contains("Right Ascension:              115.484")
    Assert assertTrue filledTemplate.contains("Declination:                  19.928")
    Assert assertTrue filledTemplate.contains("Right Ascension:              146.343")
    Assert assertTrue filledTemplate.contains("Declination:                  46.279")
  }

}

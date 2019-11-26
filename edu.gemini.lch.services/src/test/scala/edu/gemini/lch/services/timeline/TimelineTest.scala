package edu.gemini.lch.services.timeline

import java.time.{Period, ZoneId, ZonedDateTime}
import java.util.Optional

import javax.swing.{ImageIcon, JLabel, JOptionPane}
import edu.gemini.lch.model.Visibility.RiseSet
import edu.gemini.lch.model.{LaserNight, PropagationWindow, RaDecLaserTarget, Site, Visibility}
import org.junit.runner.RunWith
import org.junit.{Ignore, Test}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("/spring-services-test-context.xml"))
class TimelineTest {

  // This is a "visual" test meant to be run manually
  // It will open a window with the generated timeline for visual inspection.
  // DO NOT run it as part of the automated tests because it will block the tests waiting for someone
  // to close the window..
  @Ignore
  @Test
  def doesCreateImage() {
    val night = new LaserNight(Site.NORTH, ZonedDateTime.of(2013,1,1,18,0,0,0,ZoneId.systemDefault()), ZonedDateTime.of(2013,1,2,7,0,0,0,ZoneId.systemDefault()))
    val target = new RaDecLaserTarget(
        night,
        .0, .0,
        new Visibility(
            Optional.of(new RiseSet(ZonedDateTime.of(2013,1,1,15,0,0,0,ZoneId.systemDefault()).toInstant, ZonedDateTime.of(2013,1,2,4,0,0,0,ZoneId.systemDefault()).toInstant)),
            Optional.of(new RiseSet(ZonedDateTime.of(2013,1,1,22,0,0,0,ZoneId.systemDefault()).toInstant, ZonedDateTime.of(2013,1,2,3,0,0,0,ZoneId.systemDefault()).toInstant))
        )
    )
    target.getPropagationWindows.add(new PropagationWindow(ZonedDateTime.of(2013,1,1,19,30,0,0,ZoneId.systemDefault()), ZonedDateTime.of(2013,1,1,22,50,0,0,ZoneId.systemDefault())))
    target.getPropagationWindows.add(new PropagationWindow(ZonedDateTime.of(2013,1,1,22,51,0,0,ZoneId.systemDefault()), ZonedDateTime.of(2013,1,2,2,30,0,0,ZoneId.systemDefault())))
    target.getPropagationWindows.add(new PropagationWindow(ZonedDateTime.of(2013,1,2,2,30,5,0,ZoneId.systemDefault()), ZonedDateTime.of(2013,1,2,2,50,0,0,ZoneId.systemDefault())))
    target.getPropagationWindows.add(new PropagationWindow(ZonedDateTime.of(2013,1,2,2,50,1,0,ZoneId.systemDefault()), ZonedDateTime.of(2013,1,2,4,45,0,0,ZoneId.systemDefault())))
    target.getPropagationWindows.add(new PropagationWindow(ZonedDateTime.of(2013,1,2,5,0,0,0,ZoneId.systemDefault()), ZonedDateTime.of(2013,1,2,6,50,0,0,ZoneId.systemDefault())))

    val image =
      new TimeLineImage(night).
        withTarget(target).
        withTimes(night.getStart.minusHours(1), night.getEnd.plusHours(1)).
        withDimensions(800, 60).
        withNowMarker(new ZonedDateTime(2013,1,1,22,10,20)).
        withText(ZoneId.of("UTC"), 10).
        withBuffers(new Period(10*1000), new Period(10*1000)).
        withElevationLine()

    JOptionPane.showMessageDialog(null, new JLabel(new ImageIcon(image.bytes)))
  }
}

package edu.gemini.lch.services.impl;

import edu.gemini.lch.services.LtcsService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import scala.collection.Seq;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests some fake LTCS query responses.
 * All collisions are returned on one single line, all tokens separated by a single space.
 * We don't care about collisions with priority NO-LGS. That's how it is.
 */
public class LtcsServiceTest {

    @Test
    public void canParseEmpty() {
        // test parse method which is not on interface, test it directly
        assertEquals(0, LtcsServiceImpl.parseCollisions("", DateTime.now()).size());
        assertEquals(0, LtcsServiceImpl.parseCollisions(" ", DateTime.now()).size());
        assertEquals(0, LtcsServiceImpl.parseCollisions("NONE", DateTime.now()).size());
    }

    @Test
    public void canParse() {
        // test parse method which is not on interface, test it directly
        assertEquals(1, LtcsServiceImpl.parseCollisions(
                "GEMINI UH2.2M 10:47:08 11:19:48 UH2.2M", DateTime.now()).
                size());
        assertEquals(2, LtcsServiceImpl.parseCollisions(
                "GEMINI UH2.2M 10:47:08 11:19:48 UH2.2M " +
                        "GEMINI UH2.2M 10:47:08 11:19:48 UH2.2M", DateTime.now()).
                size());
    }

    @Test
    public void skipsNoLgs() {
        // test parse method which is not on interface, test it directly
        assertEquals(0, LtcsServiceImpl.parseCollisions(
                "GEMINI UH2.2M 10:47:08 11:19:48 NO-LGS", DateTime.now()).
                size());
        assertEquals(1, LtcsServiceImpl.parseCollisions(
                "GEMINI UH2.2M 10:47:08 11:19:48 NO-LGS " +
                        "GEMINI UH2.2M 10:47:08 11:19:48 UH2.2M", DateTime.now()).
                size());
    }

    @Test
    public void doesSort() {
        // test parse method which is not on interface, test it directly
        List<LtcsService.Collision> collisions = LtcsServiceImpl.parseCollisions(
                "GEMINI 12 12:00:08 12:19:48 UH2.2M " +
                        "GEMINI 10 10:00:08 13:19:48 UH2.2M " +
                        "GEMINI 11 11:00:08 11:19:48 UH2.2M", DateTime.now().withTimeAtStartOfDay());
        assertEquals(3, collisions.size());
        assertEquals("10", collisions.get(0).getObservatory());
        assertEquals("11", collisions.get(1).getObservatory());
        assertEquals("12", collisions.get(2).getObservatory());
    }

    @Test
    public void parsesTimesAroundNowProperly() {
        // test parse method which is not on interface, test it directly
        DateTime nowBetween = new DateTime(2013,1,1,15,0,0);
        List<LtcsService.Collision> c = LtcsServiceImpl.parseCollisions(
                "GEMINI UH2.2M 14:50:00 15:10:00 UH2.2M",
                nowBetween);
        assertEquals(nowBetween.withTimeAtStartOfDay(), c.get(0).getStart().withTimeAtStartOfDay()); // same day
        assertEquals(nowBetween.withTimeAtStartOfDay(), c.get(0).getEnd().withTimeAtStartOfDay());   // same day
    }

    @Test
    public void parsesTimesBeforeNowProperly() {
        // test parse method which is not on interface, test it directly
        // if the collision window is before the current time we move it to the next day
        // (assuming LTCS sends only collision windows in the future)
        DateTime nowAfter = new DateTime(2013,1,1,16,0,0);
        List<LtcsService.Collision> c = LtcsServiceImpl.parseCollisions(
                "GEMINI UH2.2M 14:50:00 15:10:00 UH2.2M",
                nowAfter);
        assertEquals(nowAfter.plusDays(1).withTimeAtStartOfDay(), c.get(0).getStart().withTimeAtStartOfDay()); // next day
        assertEquals(nowAfter.plusDays(1).withTimeAtStartOfDay(), c.get(0).getEnd().withTimeAtStartOfDay());   // next day
    }

    @Test
    public void parsesTimesAroundMidnightProperly() {
        // test parse method which is not on interface, test it directly
        // if the collision window wraps around midnight end has to be on the next day in the end
        DateTime now = new DateTime(2013,1,1,18,0,0);
        List<LtcsService.Collision> c = LtcsServiceImpl.parseCollisions(
                "GEMINI UH2.2M 23:55:00 00:05:00 UH2.2M",
                now);
        assertEquals(now.plusDays(0).withTimeAtStartOfDay(), c.get(0).getStart().withTimeAtStartOfDay()); // same day
        assertEquals(now.plusDays(1).withTimeAtStartOfDay(),  c.get(0).getEnd().withTimeAtStartOfDay());   // next day
    }

    @Test
    public void checkAlternativeParser1() throws Exception {
        LtcsServiceAlternativeImpl s = new LtcsServiceAlternativeImpl();
        InputStream is = getClass().getResourceAsStream("/ltcs/ltcsDetailsPage1.html");
        String page = FileCopyUtils.copyToString(new InputStreamReader(is));
        is.close();

        Seq<LtcsService.Collision> collisions = (scala.collection.Seq)s.predictions(page);
        assertEquals(1, collisions.size());
        assertEquals("SUBARU", collisions.apply(0).getObservatory());
    }

    @Test
    public void checkAlternativeParser2() throws Exception {
        LtcsServiceAlternativeImpl s = new LtcsServiceAlternativeImpl();
        InputStream is = getClass().getResourceAsStream("/ltcs/ltcsDetailsPage2.html");
        String page = FileCopyUtils.copyToString(new InputStreamReader(is));
        is.close();

        Seq<LtcsService.Collision> collisions = (scala.collection.Seq)s.predictions(page);
        assertEquals(2, collisions.size());
        assertEquals("KECK1",  collisions.apply(0).getObservatory());
        assertEquals("GEMINI", collisions.apply(1).getObservatory());
    }

    @Test
    public void alternativeCanParseSummaryPage1() throws Exception {
        LtcsServiceAlternativeImpl s = new LtcsServiceAlternativeImpl();
        InputStream is = getClass().getResourceAsStream("/ltcs/ltcsSummaryPage1.html");
        String page = FileCopyUtils.copyToString(new InputStreamReader(is));
        is.close();

        Seq<LtcsService.Collision> collisions = (scala.collection.Seq)s.collisions(page);
        assertEquals(0, collisions.size());

        Seq<LtcsService.Collision> previews = (scala.collection.Seq)s.previews(page);
        assertEquals(1, previews.size());
        assertEquals("KECK2",  previews.apply(0).getObservatory());
        assertEquals("GEMINI", previews.apply(0).getPriority());
    }

    @Test
    public void alternativeCanParseSummaryPage2() throws Exception {
        LtcsServiceAlternativeImpl s = new LtcsServiceAlternativeImpl();
        InputStream is = getClass().getResourceAsStream("/ltcs/ltcsSummaryPage2.html");
        String page = FileCopyUtils.copyToString(new InputStreamReader(is));
        is.close();

        Seq<LtcsService.Collision> collisions = (scala.collection.Seq)s.collisions(page);
        assertEquals(0, collisions.size());
        Seq<LtcsService.Collision> previews = (scala.collection.Seq)s.previews(page);
        assertEquals(0, previews.size());
    }
}

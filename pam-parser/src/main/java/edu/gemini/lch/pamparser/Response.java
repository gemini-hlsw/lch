package edu.gemini.lch.pamparser;

import edu.gemini.lch.model.PropagationWindow;
import edu.gemini.lch.model.Site;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

@SuppressWarnings("serial")
public class Response {

    private final List<Target> targets;
    private final Map<Target, List<PropagationWindow>> windows;
    private String missionId;
    private DateTime reportTime;
    private DateTime start;
    private DateTime end;

    public Response() {
        this.targets = new ArrayList<>();
        this.windows = new HashMap<>();
    }

    public Response(List<Target> targets, Map<Target, List<PropagationWindow>> windows, Site site, DateTime reportTime, DateTime start, DateTime end) {
        this.targets = targets;
        this.windows = windows;
        this.missionId = "Gemini " + site.getDisplayName();
        this.reportTime = reportTime;
        this.start = start;
        this.end = end;
    }

    public List<Target> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public List<PropagationWindow> getWindowsForTarget(Target target) {
        return Collections.unmodifiableList(windows.get(target));
    }

    public Integer getJDay() {
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.setTime(start.toDate());
        return c.get(Calendar.DAY_OF_YEAR);
    }

    public DateTime getReportTime() {
        return reportTime;
    }

    public DateTime getMissionStart() {
        return start;
    }

    public DateTime getMissionEnd() {
        return end;
    }

    public String getMissionId() {
        return missionId;
    }

    public Site getSite() {
        String id = missionId.toLowerCase();
        if (id.contains("north")) {
            return Site.NORTH;
        } else if (id.contains("south")) {
            return Site.SOUTH;
        } else {
            throw new RuntimeException("can't extract site from id: " + id);
        }
    }

    public Boolean isRaDec() {
        if (targets.isEmpty()) {
            return Boolean.TRUE; // actually we don't really know, but it shouldn't matter either
        } else {
            if (targets.get(0) instanceof RaDecTarget) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }

    public Boolean isAzEl() {
        return !isRaDec();
    }

    protected void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    protected void setReportTime(Date reportTime) {
        this.reportTime = new DateTime(reportTime);
    }

    protected void setMissionStart(Date start) {
        this.start = new DateTime(start);
    }

    protected void setMissionEnd(Date end) {
        this.end = new DateTime(end);
    }

    protected void addTarget(Target target) {
        targets.add(target);
    }

    protected void setWindowsForTarget(Target target, List<PropagationWindow> windowList) {
        windows.put(target, windowList);
    }

	public static Response parseResponse(InputStream is) throws IOException, ParseException {
		try {
			ANTLRInputStream ais = new ANTLRInputStream(is);
			ResponseLexer lexer = new ResponseLexer(ais);
			CommonTokenStream ts = new CommonTokenStream(lexer);
			ResponseParser parser = new ResponseParser(ts);
			CommonTree ct = (CommonTree) parser.response().getTree();
			CommonTreeNodeStream ns = new CommonTreeNodeStream(ct);		
			ResponseTree rt = new ResponseTree(ns);
            Response response = rt.response();

            // check for some values that must be in the parsed response
            // if the header is not found at all (e.g. when trying to parse an arbitrary text file) currently
            // the parser will just consume everything and return a Response object with all values set to null!
            // this is a catch all to avoid this - actually it would be better to improve the parser..
            // feel free to do so ;)
            Validate.notNull(response.getMissionId(),       "PAM file header does not contain a mission ID.");
            Validate.notNull(response.getMissionStart(),    "PAM file header does not contain a mission start time.");
            Validate.notNull(response.getMissionEnd(),      "PAM file header does not contain a mission end time.");
            Validate.notNull(response.getReportTime(),      "PAM file header does not contain a report time.");
            Validate.notNull(response.getJDay(),            "PAM file header does not contain a JDay.");
            Validate.notNull(response.getSite(),            "PAM file header does not contain a Site");
            Validate.notNull(response.getTargets(),         "PAM file does not contain any targets.");

            // return the response
            return response;

		} catch (RecognitionException re) {
			ParseException pe = new ParseException(re.getMessage(), re.index);
			pe.initCause(re);
			throw pe;
		}
	}
	
}



/// The response tree grammar takes the AST produced by Response.g and turns it into
/// Java objects that we can use. So the input to this thing is an AST of nodes that
/// just contain Strings. We do all the parsing in here.

tree grammar ResponseTree;

options {
	tokenVocab=Response;
	ASTLabelType=CommonTree;
}

@header {
	package edu.gemini.lch.pamparser;

	import java.text.SimpleDateFormat;
	import java.text.ParseException;
	import java.time.Instant;
	import java.time.ZonedDateTime;
	import java.time.format.DateTimeFormatter;

    import edu.gemini.lch.pamparser.Target;
    import edu.gemini.lch.pamparser.RaDecTarget;
    import edu.gemini.lch.pamparser.AzAltTarget;
    import edu.gemini.lch.model.PropagationWindow;
}

@members {
	// We need a date format to parse the date info from the WINDOW nodes.
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMMddHHmmssz");
}

/// The top-level response rule returns a Response object. It constructs this object and
/// then passes it down to the targets rule so it can be populated. At this point it's just
/// a list so it doesn't have any intrinsic member values (just children).

response returns [Response r]
@init {
	r = new Response();
}
		:	header[r]?
		    targets[r]
		;

/// the mission id comes in two separate strings because of the space between "Gemini" and "North" or "South"
header[Response r]
		:   ^(HEADER id1=STRING id2=STRING date=time start=time end=time)
		    {
		        r.setMissionId(id1.getText()+" "+id2.getText());
		        r.setReportTime(date);
		        r.setMissionStart(start);
		        r.setMissionEnd(end);
		    }
		;

/// The targets rule simply matches the target rule multiple times, passing the list on
/// down as a parameter. It doesn't return anything.

targets[Response r]
		:	^(TARGETS target[$r]*)
		;

/// The target rule accepts a list to put its result into (passed by the targets rule above).
/// Its result is, in turn, another list-like object (a Target) that also has a field
/// for coords that we need to set when the rule matches. This rule doesn't return anything.

target[Response r]
		:	^(TARGET c=coords w=windows)
			{
			    r.addTarget(c);
			    r.setWindowsForTarget(c, w);
			}
		;

/// The windows rule takes a list (passed by target above) and passes it on to the window
/// rule. It doesn't return a value.

windows returns [List<PropagationWindow> list]
@init {
	list = new ArrayList<PropagationWindow>();
}
		:	^(WINDOWS window[$list]*)
		;

/// The window rule accepts a list (passed by the windows rule above) and adds a propagation window
/// that's constructed based on the start and end times, which are Instants by this point.

window[List<PropagationWindow> list]
		:	^(WINDOW start=time end=time)
			{
				list.add(new PropagationWindow(ZonedDateTime.ofInstant(start, ZoneId.systemDefault()), ZonedDateTime.ofInstant(end, ZoneId.systemDefault())));
			}
		;

/// The time rule accepts a GMT node and returns an Instant that it constructs by
/// parsing out the string data. It uses the statically-defined DATE_FORMAT to do this.

time	returns [Instant ret]
		:	^(DATETIME yyyy=INT mm=MONTH dd=INT hhmm=INT ss=INT)
			{
			    // all times are in UTC
				String date = $yyyy.text + $mm.text + $dd.text + $hhmm.text + $ss.text + "UTC";
				try {
					ret = ZonedDateTime.parse(date, DATE_FORMAT).toInstant();
				} catch (ParseException e) {
					throw new RecognitionException();
				}
			}
        |   ^(DATETIME yyyy=INT month=MONTH dd=INT hh=INT mm=INT ss=INT)
			{
			    // all times are in UTC
				String date = $yyyy.text + $month.text + $dd.text + $hh.text + $mm.text + $ss.text + "UTC";
				try {
					ret = ZonedDateTime.parse(date, DATE_FORMAT).toInstant();
				} catch (ParseException e) {
					throw new RecognitionException();
				}
			}
		;

/// The coords rule accepts an RA_DEC node and returns a WorldCoords.

coords	returns [Target ret]
		:	^(RA_DEC ra=DECIMAL dec=DECIMAL)
			{
				ret = new RaDecTarget(Double.parseDouble($ra.text), Double.parseDouble($dec.text));
			}
		|	^(AZIMUTH az=DECIMAL el=DECIMAL)
			{
				ret = new AzAltTarget(Double.parseDouble($az.text), Double.parseDouble($el.text));
			}

		;



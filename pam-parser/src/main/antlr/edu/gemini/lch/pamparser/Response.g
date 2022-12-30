
/// This combined lexer/parser converts a Space Command response file into an AST
/// which is turned into a Response object by the ResponseTree grammar.
/// We probably don't need to do this in two steps, but I think it's a little clearer.

grammar Response;

options {
	output = AST;
	ASTLabelType = CommonTree;
}

/// These token name nodes in the AST but don't exist as real tokens in the grammar.
/// So we have to list them here.

tokens {
    HEADER;
	TARGETS;
	TARGET;
	WINDOWS;
	WINDOW;
	RA_DEC;
	AZIMUTH;
	DATETIME;
}

/// Running this thing through ANTLR produces both a lexer and a parser, so we need
/// to specify the package header for both.

@lexer::header 	{ package edu.gemini.lch.pamparser; }
@parser::header { package edu.gemini.lch.pamparser; }


/// A response is just a list of targets at this point. In the future we may parse more
/// info out of the introductory text.

response:
            header?
            targets
            ;

/// Header of the file, most of it can be ignored but we need to know report time and mission start and end time

header:
            CLASSIFICATION                                                                  // Classification: ...
            HEADER_CRAP_1                                                                   // LASER CLEARING ...
            MISSION_ID id1=STRING id2=STRING                                                // Mission ID: (followed by two strings)
            LASER_OWNER                                                                     // Laser Owner/Operator:
            REPORT_TIME time                                                                // Report Date/Time (UTC):
            MISSION_NAME                                                                    // Mission Name:
            MISSION_START time                                                              // Mission Start Date/Time (UTC):
            MISSION_STOP time                                                               // Mission Stop  Date/Time (UTC):
            MISSION_DURATION INT COLON INT COLON INT                                        // Mission Duration   (HH:MM:SS):
            STRING STRING STRING STRING STRING STRING COLON STRING STRING STRING STRING     // Type of Windows in this report:
            HEADER_CRAP_2 INT                                                               // Comment: ... Number of Targets:
            -> ^(HEADER $id1 $id2 time time time)
            ;

/// A target contains a bunch of crap that we parse and then throw away. All we really
/// care about is the RA/DEC coordinates and the windows.

targets:
            target*
            -> ^(TARGETS target*)
            ;

target	:	HEADER1
			HEADER2
			windows
			PERCENT (DECIMAL | STRING) '%'
			source_geom
			target_coord
			-> ^(TARGET target_coord windows)
		;

target_coord
        :   coords_ra_dec
        |   coords_azimuth
        ;

/// The source geometry part
source_geom :
            SOURCE_GEOMETRY
            HEADER3
			coords_fp
        ;

/// The windows rule just gathers the WINDOW nodes together under a parent node.
/// and a WINDOW node is just a start and end time. We don't care about the duration.

windows	:
            window*
            -> ^(WINDOWS window*)
        ;
window	:
            time
            time
            duration
            -> ^(WINDOW time time)
        ;

/// The GMT node is a structured date that we parse in the tree grammar.

time	:
            year=INT
			month=MONTH
			day=INT
			STRING              // (day)
			hrsmins=INT
			secs=INT
			-> ^(DATETIME $year $month $day $hrsmins $secs)
        |
            year=INT
			month=MONTH
			day=INT
			hrs=INT
			COLON
			mins=INT
			COLON
			secs=INT
			-> ^(DATETIME $year $month $day $hrs $mins $secs)
		;

/// Some simple rules for stuff we have to recognize, but don't keep.

duration:	INT COLON INT;

/// Fixed-point coordinates have to be recognized but we dont keep them.

coords_fp
		:	METHOD_FIX_PNT
			LATITUDE DECIMAL DEGREES
			LONGITUDE DECIMAL DEGREES
			ALTITUDE DECIMAL KM
		;

/// RA/DEC coordinates are turned into an RA_DEC node.

coords_ra_dec
        :   TARGET_GEOMETRY INT
            HEADER3
			METHOD_RADEC
			CATALOG_DATE
			RIGHT_ASCENSION ra=DECIMAL DEGREES
			DECLINATION dec=DECIMAL DEGREES		-> ^(RA_DEC $ra $dec)
		;

/// Azimuth coordinates are turned into an AZIMUTH node.

coords_azimuth
        :   TARGET_GEOMETRY INT
            HEADER3
            METHOD_AZIMUTH
			AZIMUTH az=DECIMAL DEGREES
			ELEVATION el=DECIMAL DEGREES		-> ^(AZIMUTH $az $el)
		;

/// Terminal tokens.

CLASSIFICATION  : 'Classification: UNCLASSIFIED';
HEADER_START    : 'UNITED STATES';
HEADER_END_1    : 'VAFB, CA 93437';
HEADER_END_2    : 'VSFB, CA 93437';
HEADER_CRAP_1   : HEADER_START (options{greedy=false;}:.)* (HEADER_END_1|HEADER_END_2);
MISSION_ID      : 'Mission ID:';
LASER_OWNER     : 'Laser Owner/Operator:' (options{greedy=false;}:~('\r'|'\n')*);
MISSION_NAME    : 'Mission Name:' (options{greedy=false;}:~('\r'|'\n')*);
REPORT_TIME     : 'Report Date/Time (UTC):';
MISSION_START   : 'Mission Start Date/Time (UTC):';
MISSION_STOP    : 'Mission Stop  Date/Time (UTC):';
MISSION_DURATION_END: '(HH:MM:SS):'|'(HHH:MM:SS):';
MISSION_DURATION: 'Mission Duration'(' ')*MISSION_DURATION_END;
COMMENT         : 'Comment:';
NUMBER_OF_TARGETS: 'Number of Targets:';
HEADER_CRAP_2   : COMMENT (options{greedy=false;}:.)* NUMBER_OF_TARGETS;
HEADER1	        : 'YYYY MMM dd (DDD) HHMM SS    YYYY MMM dd (DDD) HHMM SS      MM:SS';
HEADER2	        : '-------------------------    -------------------------    -------';
TARGET_GEOMETRY : 'Target Geometry: (WGS-84)';
SOURCE_GEOMETRY : 'Source Geometry: (WGS-84)';
HEADER3	        : '---------------';
METHOD_FIX_PNT  : 'Method: Fixed Point';
METHOD_RADEC    : 'Method: Right Ascension And Declination';
METHOD_AZIMUTH  : 'Method: Fixed Azimuth/Elevation';
CATALOG_DATE    : 'Catalog Date:    J2000';
RIGHT_ASCENSION : 'Right Ascension:';
DECLINATION     : 'Declination:';
AZIMUTH         : 'Azimuth:';
ELEVATION       : 'Elevation:';
LATITUDE        : 'Latitude:';
LONGITUDE       : 'Longitude:';
ALTITUDE        : 'Altitude:';
DEGREES         : 'degrees' (' N'|' S'|' E'|' W')?;
PERCENT         : 'Percent =';
KM              : 'km';
MONTH	        : 'Jan' | 'Feb' | 'Mar' | 'Apr' | 'May' | 'Jun' | 'Jul' | 'Aug' | 'Sep' | 'Oct' | 'Nov' | 'Dec';
DECIMAL	        : ('+' | '-')? INT '.' INT;
INT		        : '0'..'9'+;
COLON           : ':';
STRING          : ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'.'|'/'|'('|')'|','|'='|'-'|'\'')+;
WS		        : (' '|'\t'|'\r'|'\n') { skip(); };


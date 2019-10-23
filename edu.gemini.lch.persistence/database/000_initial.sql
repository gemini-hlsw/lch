----------------------------------------------------
-- Hibernate sequence reset
DROP SEQUENCE IF EXISTS hibernate_sequence;
-- Initially, we're going with a universal id
CREATE SEQUENCE hibernate_sequence START WITH 1000000;

-----------------------------------------------------------
-- Gemini laser night
DROP TABLE IF EXISTS lch_laser_nights CASCADE;
CREATE TABLE lch_laser_nights (
	id integer,
	site text,
	starts timestamp with time zone,
	ends timestamp with time zone,
	latestPrmSent timestamp with time zone,
	latestPamReceived timestamp with time zone,
	pamradecreporttime timestamp with time zone,
	pamazelreporttime timestamp with time zone
);
-- indices
ALTER TABLE lch_laser_nights ADD PRIMARY KEY (id);
CREATE INDEX lch_laser_nights_site ON lch_laser_nights(site);
CREATE INDEX lch_laser_nights_starts ON lch_laser_nights(starts);
CREATE INDEX lch_laser_nights_ends ON lch_laser_nights(ends);
-- constraints

-----------------------------------------------------------
-- Gemini lch events
DROP TABLE IF EXISTS lch_events CASCADE;
CREATE TABLE lch_events (
	id integer,
	night_id integer,
	event_time timestamp with time zone,
	message text
);
-- indices
ALTER TABLE lch_events ADD PRIMARY KEY (id);
CREATE INDEX lch_events_time ON lch_events(event_time);
-- constraints
ALTER TABLE lch_events ADD FOREIGN KEY (night_id) REFERENCES lch_laser_nights(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------------
-- Files
DROP TABLE IF EXISTS lch_files CASCADE;
CREATE TABLE lch_files (
	id integer,
	event_id integer,
	type text,
	name text,
	content text
);
-- indices
ALTER TABLE lch_files ADD PRIMARY KEY (id);
-- constraints
ALTER TABLE lch_files ADD FOREIGN KEY (event_id) REFERENCES lch_events(id) ON DELETE CASCADE DEFERRABLE;


-----------------------------------------------------------
-- Observations
DROP TABLE IF EXISTS lch_observations CASCADE;
CREATE TABLE lch_observations (
	id integer,
	type text,
	night_id integer,
	observation_id text
);
-- indices
ALTER TABLE lch_observations ADD PRIMARY KEY (id);
-- constraints
ALTER TABLE lch_observations ADD FOREIGN KEY (night_id) REFERENCES lch_laser_nights(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------------
-- Gemini laser target (corresponds to an LCH target, may cover multiple observation targets)
DROP TABLE IF EXISTS lch_laser_targets CASCADE;
CREATE TABLE lch_laser_targets (
	id integer,
	type text,
	night_id integer,
	transmitted boolean,
    degrees1 double precision,
    degrees2 double precision,
    risesAboveHorizon timestamp with time zone,
    risesAboveLimit timestamp with time zone,
    setsBelowLimit timestamp with time zone,
	setsBelowHorizon timestamp with time zone
);
-- indices
ALTER TABLE lch_laser_targets ADD PRIMARY KEY (id);
CREATE INDEX lch_laser_targets_type_index ON lch_laser_targets(type);
CREATE INDEX lch_laser_targets_degrees1_index ON lch_laser_targets(degrees1, degrees2);
-- constraints
ALTER TABLE lch_laser_targets ADD FOREIGN KEY (night_id) REFERENCES lch_laser_nights(id) ON DELETE CASCADE DEFERRABLE;

----------------------------------------------------
-- Gemini observation target visibilities
DROP TABLE IF EXISTS lch_windows CASCADE;
CREATE TABLE lch_windows (
	id integer,
	target_id integer,
	starts timestamp with time zone,
	ends timestamp with time zone
);
-- indices
ALTER TABLE lch_windows ADD PRIMARY KEY (id);
CREATE INDEX lch_window_starts_index ON lch_windows(starts);
CREATE INDEX lch_window_ends_index ON lch_windows(ends);
-- constraints
ALTER TABLE lch_windows ADD FOREIGN KEY (target_id) REFERENCES lch_laser_targets(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------------
-- Observation targets
DROP TABLE IF EXISTS lch_observation_targets CASCADE;
CREATE TABLE lch_observation_targets (
	id integer,
	type text,
	state text,
	observation_id integer,
	target_id integer,
	name text,
	targettype text,
	horizons_id integer,
    degrees1 double precision,
    degrees2 double precision
);
-- indices
ALTER TABLE lch_observation_targets ADD PRIMARY KEY (id);
-- constraints
ALTER TABLE lch_observation_targets ADD FOREIGN KEY (observation_id) REFERENCES lch_observations(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE lch_observation_targets ADD FOREIGN KEY (target_id) REFERENCES lch_laser_targets(id) ON DELETE CASCADE DEFERRABLE;

----------------------------------------------------
-- Manual closures.
DROP TABLE IF EXISTS lch_closures CASCADE;
CREATE TABLE lch_closures (
	id integer,
	night_id integer,
	starts timestamp with time zone,
	ends timestamp with time zone
);
-- indices
ALTER TABLE lch_closures ADD PRIMARY KEY (id);
CREATE INDEX lch_closure_starts_index ON lch_closures(starts);
CREATE INDEX lch_closure_ends_index ON lch_closures(ends);
-- constraints
ALTER TABLE lch_closures ADD FOREIGN KEY (night_id) REFERENCES lch_laser_nights(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------------
-- Additional engineering targets (AltAz targets)
DROP TABLE IF EXISTS lch_engineering_targets CASCADE;
CREATE TABLE lch_engineering_targets (
	id integer,
	site text,
	active boolean,
    altitude double precision,
    azimuth double precision
);
-- indices
ALTER TABLE lch_engineering_targets ADD PRIMARY KEY (id);

----------------------------------------------------
-- Holidays we need to take into account.
DROP TABLE IF EXISTS lch_holidays CASCADE;
CREATE TABLE lch_holidays (
	id integer,
	name text,
	actual date,
	observed date
);
-- indices
ALTER TABLE lch_holidays ADD PRIMARY KEY (id);
CREATE INDEX lch_holidays_index ON lch_holidays(observed);

-----------------------------------------------------------
-- LCH configuration entries
DROP TABLE IF EXISTS lch_configuration_entries CASCADE;
CREATE TABLE lch_configuration_entries (
	id integer,
	type text,
	isList bool,
	canBeEmpty bool,
	minValue double precision,
	maxValue double precision,
	regExp text,
	groupName text,
    paramName text,
    label text,
    description text
);
-- indices
ALTER TABLE lch_configuration_entries ADD PRIMARY KEY (id);
CREATE INDEX lch_configuration_entries_group_index ON lch_configuration_entries(groupName);
CREATE UNIQUE INDEX lch_configuration_entries_name_index ON lch_configuration_entries(paramName);

-----------------------------------------------------------
-- LCH configuration entries
DROP TABLE IF EXISTS lch_configuration_values CASCADE;
CREATE TABLE lch_configuration_values (
	id integer,
	entry_id integer,
	site text,
    paramValue text
);
-- indices
ALTER TABLE lch_configuration_values ADD PRIMARY KEY (id);
-- constraints
ALTER TABLE lch_configuration_values ADD FOREIGN KEY (entry_id) REFERENCES lch_configuration_entries(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------------
-----------------------------------------------------------
-- load with default configuration data
COPY lch_configuration_entries (id, type, isList, canBeEmpty, minValue, maxValue, regExp, groupName, paramName, label, description) from stdin with delimiter ',';
100,DOUBLE,false,false,1,1,x,Visibility,VISIBILITY_MIN_ALTITUDE,Minimal Altitude,Minimal altitude in degrees.
101,DOUBLE,false,false,1,1,x,Sidereal,NEARBY_GROUP_MAX_DISTANCE,Maximal Distance,Maximal distance between observations represented by a single laser target in degrees.
102,INTEGER,false,false,60,3600,x,Horizons,HORIZONS_STEP_WIDTH,Step Width,Step width for horizons service in arcseconds (must be between 60 and 3600).
103,INTEGER,false,false,10,1,x,Emails,NEW_MAIL_POLL_INTERVAL,Poll Intervall Duration,Duration between checks for incoming emails in seconds (bigger than 10).
104,TEXT,false,false,1,1,x,PRM,PRM_HEADER_TEMPLATE,Header Template,Template for PRM file header.
105,TEXT,false,false,1,1,x,PRM,PRM_RADEC_TARGET_TEMPLATE,RaDec Target Template,Template for PRM file ra/dec targets. Will be repeated for each target.
106,TEXT,false,false,1,1,x,PRM,PRM_AZEL_TARGET_TEMPLATE,AzEl Target Template,Template for PRM file az/el targets. Will be repeated for each target.
107,TEXT,false,false,1,1,x,PRM,PRM_FILENAME_TEMPLATE,File Name Template,Template for PRM file name.
108,TEXT,false,false,1,1,x,PRM,PRM_TARGET_SEPARATOR_TEMPLATE,Target Separation Template,Template used between targets. Will be repeated after each target except for last one.
109,TEXT,false,false,1,1,x,PRM,PRM_FOOTER_TEMPLATE,Footer Template,Template for footer of PRM file. Will be used after last target.
110,STRING,false,false,1,1,x,Emails,EMAILS_LCH_TO_ADDRESSES,LCH Email,Email address to be used for emails going to LCH.
111,STRING,true,false,1,1,x,Emails,EMAILS_INTERNAL_TO_ADDRESSES,Internal Email,Internal email address to be used for error messages and informational emails.
112,STRING,false,false,1,1,x,Emails,EMAILS_ACCOUNT_USER,LTTS Email User,User name for LTTS email account.
113,STRING,false,false,1,1,x,Emails,EMAILS_ACCOUNT_PASSWORD,LTTS Email Password,Password for LTTS email account.
114,STRING,false,false,1,1,x,Emails,EMAILS_FROM_ADDRESS,From Address,From address to be used for all emails sent by LTTS.
115,PERIOD,true,false,1,1,x,Scheduler,SCHEDULER_PROCESS_NIGHTS_SCHEDULE,Process nights,Daily schedule used to update and process nights. This includes creating and sending PRM files and generating warnings for unsent targets and overdue PAM files.
116,STRING,true,false,1,1,x,Emails,EMAILS_LCH_CC_ADDRESSES,LCH Email CCs,Email cc addresses to be used for emails going to LCH.
117,STRING,true,false,1,1,x,Emails,EMAILS_LCH_BCC_ADDRESSES,LCH EmailBCCs,Email bcc addresses to be used for emails going to LCH.
118,STRING,true,false,1,1,x,Emails,EMAILS_INTERNAL_CC_ADDRESSES,Internal Email CC,Internal email bcc addresses to be used for error messages and informational emails.
119,STRING,true,false,1,1,x,Emails,EMAILS_INTERNAL_BCC_ADDRESSES,Internal Email BCC,Internal email bcc addresses to be used for error messages and informational emails.
120,STRING,false,false,1,1,x,Emails,EMAILS_PRM_EMAIL_SUBJECT_TEMPLATE,PRM Email Subject,Template for the subject of emails used to send PRM files to LCH.
121,TEXT,false,false,1,1,x,Emails,EMAILS_PRM_EMAIL_BODY_TEMPLATE,PRM Email Body,Template for the body of emails used to send PRM files to LCH.
122,STRING,false,false,1,1,x,Emails,EMAILS_PRM_ADDENDUM_EMAIL_SUBJECT_TEMPLATE,PRM Addendum Email Subject,Template for the subject of emails used to send addendum PRM files to LCH.
123,TEXT,false,false,1,1,x,Emails,EMAILS_PRM_ADDENDUM_EMAIL_BODY_TEMPLATE,PRM Addendum Email Body,Template for the body of emails used to send addendum PRM files to LCH.
124,STRING,false,false,1,1,x,Emails,EMAILS_PAM_MISSING_EMAIL_SUBJECT_TEMPLATE,Missing PAM Email Subject,Template for the subject of emails used to warn when PAM file for a night is late.
125,TEXT,false,false,1,1,x,Emails,EMAILS_PAM_MISSING_EMAIL_BODY_TEMPLATE,Missing PAM Email Body,Template for the body of emails used to warn when PAM file for a night is late.
126,INTEGER,false,false,1,1,x,PRM,PRM_MAX_NUMBER_OF_TARGETS,Maximum targets,The maximum number of targets per File.
128,INTEGER,false,false,1,1,x,Visibility,VISIBILITY_MIN_DURATION,Minimal Duration,Minimal duration in minutes a target has to be above the minimal altitude to be considered.
129,SELECTION,true,false,1,1,Twilight,Visibility,VISIBILITY_TWILIGHT,Twilight,Name of twilight that is considered as start or end for visibility of targets.
130,STRING,false,false,1,1,x,Miscellaneous,HELP_URL,Help URL,URL of web page providing help for LTTS.
131,STRING,false,false,1,1,x,Miscellaneous,ODB_URL,ODB URL,URL of server hosting the ODB.
132,STRING,true,true,1,1,x,Miscellaneous,SCIENCE_QUERY,ODB Science Query,Query for getting the science targets from the ODB.
133,STRING,true,true,1,1,x,Miscellaneous,ENGINEERING_QUERY,ODB Engineering Query,Query for getting the engineering targets from the ODB.
134,INTEGER,false,true,1,1,x,Miscellaneous,TIME_ZONE_OFFSET,Time Zone Offset,Number of hours local time is offset to GMT/UTC time; if empty the system time of the server is used.
135,STRING,false,false,1,1,x,Emails,EMAILS_NEW_TARGETS_EMAIL_SUBJECT_TEMPLATE,New Laser Targets Email Subject,Template for the subject of emails used to warn when there are unsent laser targets.
136,TEXT,false,false,1,1,x,Emails,EMAILS_NEW_TARGETS_EMAIL_BODY_TEMPLATE,New Laser Targets Email Body,Template for the body of emails used to warn when there are unsent laser targets.
137,INTEGER,false,false,1,1,x,Emails,EMAILS_PRM_SEND_WORK_DAYS_AHEAD,Send Limit for PRM Files,Number of work days before start of laser night PRM files are sent to LCH.
138,STRING,false,false,1,1,x,Miscellaneous,LTCS_URL,LTCS URL,URL of server hosting the LTCS.
139,INTEGER,false,false,0,1,x,LIS,LIS_BUFFER_BEFORE_SHUTTER_WINDOW,Safety buffer before,Seconds the laser should be shuttered before the actual shuttering window.
140,INTEGER,false,false,0,1,x,LIS,LIS_BUFFER_AFTER_SHUTTER_WINDOW,Safety buffer after,Seconds the laser should be kept shuttered after the actual shuttering window.
141,STRING,false,false,1,1,x,Emails,EMAILS_REPLY_TO_ADDRESS,Reply-To Address,Reply-To address set in outgoing emails.
142,STRING,false,false,1,1,x,Miscellaneous,EPICS_ADDRESS_LIST,EPICS Address List,A list of IP addresses for communicating with EPICS.
143,STRING,true,true,1,1,x,Miscellaneous,TEST_SCIENCE_QUERY,ODB Test Science Query,Query for getting the test science targets from the ODB.
144,DOUBLE,false,false,1,3600,x,LIS,ERROR_CONE_ANGLE,Error Cone Size,Diameter of error cone in arcseconds.
\.

-- (NOTE: every parameter must exist for each site!)
COPY lch_configuration_values (id, entry_id, site, paramValue) from stdin with delimiter ',';
500,100,NORTH,35.0
501,100,SOUTH,35.0
502,101,NORTH,0.035
503,101,SOUTH,0.035
504,102,NORTH,120
505,102,SOUTH,120
506,103,NORTH,60
507,103,SOUTH,60
508,107,NORTH,PRM_Gemini North_589nm_14W_884nrad_76MHz_${{TARGET-TYPE}}_${{NIGHT-START_UTC_yyyyMMdd}}${{FILE-NUMBER}}.txt
509,107,SOUTH,PRM_Gemini South_589nm_50W_884nrad_76MHz_${{TARGET-TYPE}}_${{NIGHT-START_UTC_yyyyMMdd}}${{FILE-NUMBER}}.txt
510,108,NORTH,\n
511,108,SOUTH,\n
512,109,NORTH,END OF FILE
513,109,SOUTH,END OF FILE
514,110,NORTH,fnussberger@gemini.edu
515,110,SOUTH,fnussberger@gemini.edu
516,111,NORTH,fnussberger@gemini.edu
517,111,SOUTH,fnussberger@gemini.edu
518,112,NORTH,gn-lch
519,112,SOUTH,gs-lch
520,113,NORTH,LaserGuide*
521,113,SOUTH,LaserGuide*
522,114,NORTH,fnussberger@gemini.edu
523,114,SOUTH,fnussberger@gemini.edu
524,115,NORTH,06:00:00
525,115,SOUTH,06:00:00
526,116,NORTH,
527,116,SOUTH,
528,117,NORTH,
529,117,SOUTH,
530,118,NORTH,
531,118,SOUTH,
532,119,NORTH,
533,119,SOUTH,
534,120,NORTH,Gemini North Observatory Predictive Avoidance Request for the Night ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}}
535,120,SOUTH,Gemini South Observatory Predictive Avoidance Request for the Night ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}}
536,122,NORTH,Addendum: Gemini North Observatory Predictive Avoidance Request for the Night ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}}
537,122,SOUTH,Addendum: Gemini South Observatory Predictive Avoidance Request for the Night ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}}
538,124,NORTH,LTTS: WARNING: PAM file for night ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}} is missing!
539,124,SOUTH,LTTS: WARNING: PAM file for night ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}} is missing!
540,126,NORTH,150
541,126,SOUTH,150
544,128,NORTH,30
545,128,SOUTH,30
546,129,NORTH,CIVIL
547,129,SOUTH,CIVIL
556,134,NORTH,
557,134,SOUTH,
548,130,NORTH,http://swg.wikis-internal.gemini.edu/index.php/LTTS
549,130,SOUTH,http://swg.wikis-internal.gemini.edu/index.php/LTTS
550,131,NORTH,http://gnodb.hi.gemini.edu:8442/odbbrowser/targets
551,131,SOUTH,http://gsodb.cl.gemini.edu:8442/odbbrowser/targets
560,135,NORTH,LTTS: Night ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}} has unsent laser targets
561,135,SOUTH,LTTS: Night ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}} has unsent laser targets
562,137,NORTH,4
563,137,SOUTH,4
564,138,NORTH,http://mko-ltcs/ltcs/screens/query.php
565,138,SOUTH,http://cpltcs01.cl.gemini.edu/ltcs/screens/query.php
566,139,NORTH,10
567,139,SOUTH,10
568,140,NORTH,10
569,140,SOUTH,10
570,141,NORTH,fnussberger@gemini.edu
571,141,SOUTH,fnussberger@gemini.edu
572,142,NORTH,10.2.2.255
573,142,SOUTH,172.17.2.255
574,144,NORTH,600
575,144,SOUTH,700
\.

INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (552, 132, 'NORTH', 'programSemester=${{SEMESTER_-2}}|${{SEMESTER_-1}}|${{SEMESTER}}|${{SEMESTER_1}},programActive=Yes,observationAo=Altair + LGS,observationStatus=Phase 2|For Review|In Review|For Activation|On Hold|Ready|Ongoing|Inactive,observationClass=Science|Nighttime Partner Calibration|Nighttime Program Calibration');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (553, 132, 'SOUTH', 'programSemester=${{SEMESTER_-2}}|${{SEMESTER_-1}}|${{SEMESTER}}|${{SEMESTER_1}},programActive=Yes,observationInstrument=GSAOI,observationStatus=Phase 2|For Review|In Review|For Activation|On Hold|Ready|Ongoing|Inactive,observationClass=Science|Nighttime Partner Calibration|Nighttime Program Calibration');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (554, 133, 'NORTH', 'programReference=Chad-LGS,observationStatus=Ready');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (555, 133, 'SOUTH', 'programReference=TO_BE_DEFINED');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (558, 143, 'NORTH', 'programReference=LTTS-Test-Targets');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (559, 143, 'SOUTH', 'programReference=LTTS-Test-Targets');

-- ===================== PAM TEMPLATES =====================

-- GN PAM HEADER AND TARGETS TEMPLATE
INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (700, 104, 'NORTH',
'Classification:             Unclassified
File Name:                  ${{FILE-NAME}}
Message Purpose:            Request for Predictive Avoidance Support
Message Date/Time (UTC):    ${{NOW_UTC_yyyy MMM dd (DDD) HH:mm:ss}}
Type Windows Requested:     Open
Point of Contact:           Gemini System Support Associate on duty at Mauna Kea summit
                            (Voice) (808) 974 2650
                            (Fax) (808) 974 2589
                            (E-mail) gnlgsops@gemini.edu
Emergency Phone # at Operations Site: (808) 974 2650
Remarks: Targets for date  ${{NIGHT-START_UTC_yyyy MMM dd (DDD) z}}.

MISSION INFORMATION
---------------------------
Owner/Operator:               Gemini North Observatory
Mission Name/Number:          Gemini North_589nm_14W_884nrad_76MHz
Target Type:                  Right Ascension and Declination
Location:                     Gemini North Observatory, Mauna Kea, Hawaii, USA
Start Date/Time (UTC):        ${{NIGHT-START_UTC_yyyy MMM dd (DDD) HH:mm:ss}}
End Date/Time (UTC):          ${{NIGHT-END_UTC_yyyy MMM dd (DDD) HH:mm:ss}}
Duration (HH:MM:SS):          ${{NIGHT-DURATION}}

LASER INFORMATION
---------------------------
Laser:                        Gemini North_589nm_14W_884nrad_76MHz

SOURCE INFORMATION
------------------------------------
Method:                       Fixed Point
Latitude:                     19.8238 degrees N
Longitude:                    155.4690 degrees W
Altitude:                     4.2134 km

TARGET INFORMATION
------------------------------------
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (701, 105, 'NORTH',
'Method:                       Right Ascension and Declination
Catalog Date:                 J2000
Right Ascension:              ${{TARGET-RA-DEGREES_%.3f}}
Declination:                  ${{TARGET-DEC-DEGREES_%.3f}}
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (702, 106, 'NORTH',
'Method:                       Fixed Azimuth/Elevation
Azimuth:                      ${{TARGET-AZ-DEGREES_%.3f}}
Elevation:                    ${{TARGET-EL-DEGREES_%.3f}}
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (703, 121, 'NORTH',
'Aloha

Attached please find the Predictive Avoidance Request for the night of ${{NIGHT-START_UTC_EEEE, MMM dd, yyyy (DDD) z}}.

Thank you.
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (704, 123, 'NORTH',
'Aloha

Attached please find an Addendum to the Predictive Avoidance Request which was
forwarded to your office on ${{NIGHT-PRM-SENT_UTC_EEEE, MMM dd, yyyy (DDD) HH:mm z}}.

This addendum contains the original list along with the additional
targets for the night of ${{NIGHT-START_UTC_EEEE, MMM dd, yyyy (DDD) HH:mm z}}.

Should you have any questions, please contact our office for resolution.
Thanks.
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (705, 125, 'NORTH',
'No clearance windows for the laser night starting on

  ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}}
  ${{NIGHT-START_LOCAL_yyyy-MM-dd (DDD) z}}

have been received so far.

Please contact Space Command.
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (706, 136, 'NORTH',
'The set of observation targets has been changed and new laser targets have been created to cover them which have not yet been sent to LCH.
Consider resending PRMs.
');


-- ======================


-- GS PAM HEADER AND TARGETS TEMPLATE
INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (800, 104, 'SOUTH',
'Classification:             Unclassified
File Name:                  ${{FILE-NAME}}
Message Purpose:            Request for Predictive Avoidance Support
Message Date/Time (UTC):    ${{NOW_UTC_yyyy MMM dd (DDD) HH:mm:ss}}
Type Windows Requested:     Open
Point of Contact:           Gemini System Support Associate on duty at Cerro Pachon summit
                            (Voice) +56 (51) 205-701
                            (Fax) +56 (51) 205-650
                            (E-mail) gslgsops@gemini.edu
Emergency Phone # at Operations Site :+56 (51) 205-701
Remarks: Targets for date  ${{NIGHT-START_UTC_yyyy MMM dd (DDD) z}}.

MISSION INFORMATION
---------------------------
Owner/Operator:               Gemini South Observatory
Mission Name/Number:          Gemini South_589nm_50W_884nrad_76MHz
Target Type:                  Right Ascension and Declination
Location:                     Gemini South Observatory, Cerro Pachon, Coquimbo, CHILE
Start Date/Time (UTC):        ${{NIGHT-START_UTC_yyyy MMM dd (DDD) HH:mm:ss}}
End Date/Time (UTC):          ${{NIGHT-END_UTC_yyyy MMM dd (DDD) HH:mm:ss}}
Duration (HH:MM:SS):          ${{NIGHT-DURATION}}

LASER INFORMATION
---------------------------
Laser:                        Gemini South_589nm_50W_884nrad_76MHz

SOURCE INFORMATION
------------------------------------
Method:                       Fixed Point
Latitude:                     30.2408 degrees S
Longitude:                    70.7367 degrees W
Altitude:                     2.722 km

TARGET INFORMATION
------------------------------------
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (801, 105, 'SOUTH',
'Method:                       Right Ascension and Declination
Catalog Date:                 J2000
Right Ascension:              ${{TARGET-RA-DEGREES_%.3f}}
Declination:                  ${{TARGET-DEC-DEGREES_%.3f}}
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (802, 106, 'SOUTH',
'Method:                       Fixed Azimuth/Elevation
Azimuth:                      ${{TARGET-AZ-DEGREES_%.3f}}
Elevation:                    ${{TARGET-EL-DEGREES_%.3f}}
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (803, 121, 'SOUTH',
'To Whom It May Concern,

Attached please find the Predictive Avoidance Request for the night of ${{NIGHT-START_UTC_EEEE, MMM dd, yyyy (DDD) z}}.

Thank you.
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (804, 123, 'SOUTH',
'To Whom It May Concern,

Attached please find an Addendum to the Predictive Avoidance Request which was
forwarded to your office on ${{NIGHT-PRM-SENT_UTC_EEEE, MMM dd, yyyy (DDD) HH:mm z}}.

This addendum contains the original list along with the additional
targets for the night of ${{NIGHT-START_UTC_EEEE, MMM dd, yyyy (DDD) HH:mm z}}.

Should you have any questions, please contact our office for resolution.
Thanks.
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (805, 125, 'SOUTH',
'No clearance windows for the laser night starting on

  ${{NIGHT-START_UTC_yyyy-MM-dd (DDD) z}}
  ${{NIGHT-START_LOCAL_yyyy-MM-dd (DDD) z}}

have been received so far.

Please contact Space Command.
');

INSERT INTO lch_configuration_values (id, entry_id, site, paramValue) VALUES (806, 136, 'SOUTH',
'The set of observation targets has been changed and new laser targets have been created to cover them which have not yet been sent to LCH.
Consider resending PRMs.
');

-- Engineering targets
COPY lch_engineering_targets (id, site, active, altitude, azimuth) from stdin with delimiter ',';
900,NORTH,true,90.0,0.0
950,SOUTH,true,90.0,0.0
951,SOUTH,true,85.0,0.0
952,SOUTH,true,85.0,90.0
953,SOUTH,true,85.0,180.0
954,SOUTH,true,85.0,270.0
955,SOUTH,true,80.0,0.0
956,SOUTH,true,80.0,90.0
957,SOUTH,true,80.0,180.0
958,SOUTH,true,80.0,270.0
959,SOUTH,true,75.0,0.0
960,SOUTH,true,75.0,90.0
961,SOUTH,true,75.0,180.0
962,SOUTH,true,75.0,270.0
963,SOUTH,true,70.0,0.0
964,SOUTH,true,70.0,90.0
965,SOUTH,true,70.0,180.0
966,SOUTH,true,70.0,270.0
967,SOUTH,true,65.0,0.0
968,SOUTH,true,65.0,90.0
969,SOUTH,true,65.0,180.0
970,SOUTH,true,65.0,270.0
971,SOUTH,true,60.0,0.0
972,SOUTH,true,60.0,90.0
973,SOUTH,true,60.0,180.0
974,SOUTH,true,60.0,270.0
975,SOUTH,true,55.0,0.0
976,SOUTH,true,55.0,90.0
977,SOUTH,true,55.0,180.0
978,SOUTH,true,55.0,270.0
979,SOUTH,true,50.0,0.0
980,SOUTH,true,50.0,90.0
981,SOUTH,true,50.0,180.0
982,SOUTH,true,50.0,270.0
983,SOUTH,true,45.0,0.0
984,SOUTH,true,45.0,90.0
985,SOUTH,true,45.0,180.0
986,SOUTH,true,45.0,270.0
\.



-- US FEDERAL HOLIDAYS; Source: http://www.opm.gov/Operating_Status_Schedules/fedhol/2012.asp
COPY lch_holidays (id, name, actual, observed) from stdin with delimiter ';';
1000;New Year’s Day;2012-01-01;2012-01-02
1001;Birthday of Martin Luther King, Jr.;2012-01-16;2012-01-16
1002;Washington’s Birthday;2012-02-20;2012-02-20
1003;Memorial Day;2012-05-28;2012-05-28
1004;Independence Day;2012-07-04;2012-07-04
1005;Labor Day;2012-09-03;2012-09-03
1006;Columbus Day;2012-10-08;2012-10-08
1007;Veterans Day;2012-11-11;2012-11-12
1008;Thanksgiving Day;2012-11-22;2012-11-22
1009;Christmas Day;2012-12-25;2012-12-25
1010;New Year’s Day;2013-01-01;2013-01-01
1011;Birthday of Martin Luther King, Jr.;2013-01-21;2013-01-21
1012;Washington’s Birthday;2013-02-18;2013-02-18
1013;Memorial Day;2013-05-27;2013-05-27
1014;Independence Day;2013-07-04;2013-07-04
1015;Labor Day;2013-09-02;2013-09-02
1016;Columbus Day;2013-10-14;2013-10-14
1017;Veterans Day;2013-11-11;2013-11-11
1018;Thanksgiving Day;2013-11-28;2013-11-28
1019;Christmas Day;2013-12-25;2013-12-25
1020;New Year’s Day;2014-01-01;2014-01-01
1021;Birthday of Martin Luther King, Jr.;2014-01-20;2014-01-20
1022;Washington’s Birthday;2014-02-17;2014-02-17
1023;Memorial Day;2014-05-26;2014-05-26
1024;Independence Day;2014-07-04;2014-07-04
1025;Labor Day;2014-09-01;2014-09-01
1026;Columbus Day;2014-10-13;2014-10-13
1027;Veterans Day;2014-11-11;2014-11-11
1028;Thanksgiving Day;2014-11-27;2014-11-27
1029;Christmas Day;2014-12-25;2014-12-25
1030;New Year’s Day;2015-01-01;2015-01-01
1031;Birthday of Martin Luther King, Jr.;2015-01-19;2015-01-19
1032;Washington’s Birthday;2015-02-16;2015-02-16
1033;Memorial Day;2015-05-25;2015-05-25
1034;Independence Day;2015-07-04;2015-07-03
1035;Labor Day;2015-09-07;2015-09-07
1036;Columbus Day;2015-10-12;2015-10-12
1037;Veterans Day;2015-11-11;2015-11-11
1038;Thanksgiving Day;2015-11-26;2015-11-26
1039;Christmas Day;2015-12-25;2015-12-25
1040;New Year’s Day;2016-01-01;2016-01-01
1041;Birthday of Martin Luther King, Jr.;2016-01-18;2016-01-18
1042;Washington’s Birthday;2016-02-15;2016-02-15
1043;Memorial Day;2016-05-30;2016-05-30
1044;Independence Day;2016-07-04;2016-07-04
1045;Labor Day;2016-09-05;2016-09-05
1046;Columbus Day;2016-10-10;2016-10-10
1047;Veterans Day;2016-11-11;2016-11-11
1048;Thanksgiving Day;2016-11-24;2016-11-24
1049;Christmas Day;2016-12-25;2016-12-26
1050;New Year’s Day;2017-01-01;2017-01-02
1051;Birthday of Martin Luther King, Jr.;2017-01-16;2017-01-16
1052;Washington’s Birthday;2017-02-20;2017-02-20
1053;Memorial Day;2017-05-29;2017-05-29
1054;Independence Day;2017-07-04;2017-07-04
1055;Labor Day;2017-09-04;2017-09-04
1056;Columbus Day;2017-10-09;2017-10-09
1057;Veterans Day;2017-11-11;2017-11-10
1058;Thanksgiving Day;2017-11-23;2017-11-23
1059;Christmas Day;2017-12-25;2017-12-25
1060;New Year’s Day;2018-01-01;2018-01-01
1061;Birthday of Martin Luther King, Jr.;2018-01-15;2018-01-15
1062;Washington’s Birthday;2018-02-19;2018-02-19
1063;Memorial Day;2018-05-28;2018-05-28
1064;Independence Day;2018-07-04;2018-07-04
1065;Labor Day;2018-09-03;2018-09-03
1066;Columbus Day;2018-10-08;2018-10-08
1067;Veterans Day;2018-11-11;2018-11-12
1068;Thanksgiving Day;2018-11-22;2018-11-22
1069;Christmas Day;2018-12-25;2018-12-25
\.

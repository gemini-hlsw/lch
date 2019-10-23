----------------------------------------------------------------------------------------------------------------------
-- Database updates needed to bring 1.2.0 up to 1.3.0

----------------------------------------------------------------------------------------------------------------------
--
-- LCH-295: Add password protection for administration part of application
--
----------------------------------------------------------------------------------------------------------------------

-- add new configuration entry for users with admin privileges
INSERT INTO lch_configuration_entries(id, type, isList, canBeEmpty, minValue, maxValue, regExp, groupName, paramName, label, description) VALUES (145, 'STRING', true, false, 1, 1, 'x', 'Miscellaneous', 'ADMIN_USERS', 'Administrators', 'Coma separated list of user names with administrator privileges');

-- add new configuration entry for users with admin privileges
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (576, 145, 'NORTH', 'anitta, astephens, dwalther');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (577, 145, 'SOUTH', 'ppessev, aserio, emarin');


----------------------------------------------------------------------------------------------------------------------
--
-- LCH-299: New implementation of LTCS service needs host only, rest of URL is constructed by service.
--
----------------------------------------------------------------------------------------------------------------------

UPDATE lch_configuration_values SET paramValue = 'http://mko-ltcs.hi.gemini.edu' where id = 564;
UPDATE lch_configuration_values SET paramValue = 'http://cpltcs01.cl.gemini.edu' where id = 565;


----------------------------------------------------------------------------------------------------------------------
--
-- LCH-300: Add EPICS TCS simulator IP addresses.
--
----------------------------------------------------------------------------------------------------------------------

UPDATE lch_configuration_values SET paramValue = '10.2.2.255 10.1.2.179' where id = 572;
UPDATE lch_configuration_values SET paramValue = '172.17.2.255 172.16.2.20' where id = 573;


----------------------------------------------------------------------------------------------------------------------
--
-- LCH-303: Make sure that on reception of multiple PAM files newer propagation windows are not overwritten
--          with older ones. In order to do that the PAM timestamp is not stored per night anymore but per
--          laser target, this allows to deal with reception of multiple PAM files properly.
--
----------------------------------------------------------------------------------------------------------------------

ALTER TABLE lch_laser_nights DROP COLUMN pamradecreporttime;
ALTER TABLE lch_laser_nights DROP COLUMN pamazelreporttime;

ALTER TABLE lch_laser_targets ADD COLUMN windowsTimestamp TIMESTAMP WITH TIME ZONE;


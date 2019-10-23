----------------------------------------------------------------------------------------------------------------------
-- Database updates needed to bring 1.3.1 up to 1.4.0
----------------------------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------------------------
-- lch_files: two new columns to store the folder_id and the file_id used by the new JSpOC interchange format.
----------------------------------------------------------------------------------------------------------------------

ALTER TABLE lch_files ADD folder_id int DEFAULT 0;
ALTER TABLE lch_files ADD file_id int DEFAULT 0;
CREATE INDEX lch_files_id ON lch_files(folder_id,file_id);

----------------------------------------------------------------------------------------------------------------------
--
-- Add two new configuration parameters for dealing with space-track.org web page.
--
----------------------------------------------------------------------------------------------------------------------

-- Define a pattern that is used to decide which emails will trigger re-reading new PAM files from web page.
INSERT INTO lch_configuration_entries(id, type, isList, canBeEmpty, minValue, maxValue, regExp, groupName, paramName, label, description) VALUES (146, 'STRING', false, false, 1, 1, 'x', 'Emails', 'SPACE_TRACK_EMAIL', 'Space Track Email Pattern', 'Incoming emails from an address containing this string will trigger download of new PAM files from space track web page.');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (578, 146, 'NORTH', 'space-track.org');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (579, 146, 'SOUTH', 'space-track.org');

-- Define which files have been processed, this value should *not* be changed manually but in case we need a manual
-- intervention to re-process files for some reason: this is how to do it.
INSERT INTO lch_configuration_entries(id, type, isList, canBeEmpty, minValue, maxValue, regExp, groupName, paramName, label, description) VALUES (147, 'INTEGER', false, false, 1, 1, 'x', 'Emails', 'SPACE_TRACK_MAX_FILE_ID', 'Current Space Track File ID', 'Maximum ingested file id from space track web page. Only change this if you know what you are doing!');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (580, 147, 'NORTH', '0');
INSERT INTO lch_configuration_values(id, entry_id, site, paramValue) VALUES (581, 147, 'SOUTH', '0');

----------------------------------------------------------------------------------------------------------------------
--
-- Previous PRM file names are too long for space-track.org, use a shorter naming pattern (including a timestamp to
-- make file name unique because we can not replace already existing files on the web page).
--
----------------------------------------------------------------------------------------------------------------------

UPDATE lch_configuration_values SET paramValue = 'PRM_GN_${{TARGET-TYPE}}_${{NIGHT-START_UTC_DDD}}_${{NOW_UTC_yyyyMMddHHmmss}}${{FILE-NUMBER}}.txt' where id = 508;
UPDATE lch_configuration_values SET paramValue = 'PRM_GS_${{TARGET-TYPE}}_${{NIGHT-START_UTC_DDD}}_${{NOW_UTC_yyyyMMddHHmmss}}${{FILE-NUMBER}}.txt' where id = 509;

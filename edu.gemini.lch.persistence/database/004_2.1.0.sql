----------------------------------------------------------------------------------------------------------------------
-- Database updates needed for version 2.1.0
----------------------------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------------------------
--
-- When moving from Exchange to Google Mail in June 2015 the email usernames and passwords changed.
--
----------------------------------------------------------------------------------------------------------------------

UPDATE lch_configuration_values SET paramValue = 'gn-lch@gemini.edu' where id = 518;
UPDATE lch_configuration_values SET paramValue = 'Laser guide star*' where id = 520;

UPDATE lch_configuration_values SET paramValue = 'gs-lch@gemini.edu' where id = 519;
UPDATE lch_configuration_values SET paramValue = 'Laser guide star*' where id = 521;


----------------------------------------------------------------------------------------------------------------------
--
-- Update naming convention used for PRM files.
--
----------------------------------------------------------------------------------------------------------------------

UPDATE lch_configuration_values SET paramValue = 'PRM_GN_Gemini_North_${{NOW_UTC_ddMMMyyyy}}_For_JDAY${{NIGHT-START_UTC_DDD}}_${{TARGET-TYPE}}${{FILE-NUMBER}}.txt' where id = 508;
UPDATE lch_configuration_values SET paramValue = 'PRM_GS_Gemini_South_${{NOW_UTC_ddMMMyyyy}}_For_JDAY${{NIGHT-START_UTC_DDD}}_${{TARGET-TYPE}}${{FILE-NUMBER}}.txt' where id = 509;

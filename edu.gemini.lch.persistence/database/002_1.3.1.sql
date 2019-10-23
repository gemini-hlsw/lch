----------------------------------------------------------------------------------------------------------------------
-- Database updates needed to bring 1.3.0 up to 1.3.1

----------------------------------------------------------------------------------------------------------------------
--
-- Add indices for foreign key relationships to avoid expensive table scans.
-- These indices should have been added from the start, but I was - wrongly - assuming that foreign key
-- relationships result in indices. This is wrong. Only primary key and unique constraints are automatically
-- translated in corresponding indices.
--
----------------------------------------------------------------------------------------------------------------------


CREATE INDEX lch_events_night_index ON lch_events(night_id);
CREATE INDEX lch_files_event_index ON lch_files(event_id);
CREATE INDEX lch_observations_night_index ON lch_observations(night_id);
CREATE INDEX lch_laser_targets_night_index ON lch_laser_targets(night_id);
CREATE INDEX lch_window_target_index ON lch_windows(target_id);
CREATE INDEX lch_observation_targets_observation_index ON lch_observation_targets(observation_id);
CREATE INDEX lch_observation_targets_target_index ON lch_observation_targets(target_id);
CREATE INDEX lch_closures_night_index ON lch_closures(night_id);
CREATE INDEX lch_configuration_values_entry_index ON lch_configuration_values(entry_id);



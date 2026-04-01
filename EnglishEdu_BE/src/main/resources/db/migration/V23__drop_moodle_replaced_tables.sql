-- V23: Drop tables that are now handled by Moodle LMS
-- course_modules, lessons, lesson_progress → Moodle core_course_get_contents + completion tracking
-- timeline_events → Moodle core_calendar_get_action_events_by_timesort
-- calendar_events → Moodle core_calendar_get_calendar_events
-- course_schedules → Moodle calendar events

-- Drop in dependency order (child tables first)
DROP TABLE IF EXISTS lesson_progress;
DROP TABLE IF EXISTS lessons;
DROP TABLE IF EXISTS course_modules;
DROP TABLE IF EXISTS timeline_events;
DROP TABLE IF EXISTS calendar_events;
DROP TABLE IF EXISTS course_schedules;

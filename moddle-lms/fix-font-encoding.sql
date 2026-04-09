-- =============================================================================
-- ERR-16: Fix double-encoded UTF-8 characters in Moodle quiz content
-- Cause: SQL exported with latin1 charset then re-imported as utf8mb4
-- Result: Characters like ┬á (U+252C U+00E1) appear instead of proper UTF-8
-- =============================================================================
-- Usage on server:
--   docker exec -i <mariadb-container> mariadb -u bn_moodle -pbn_pass bitnami_moodle < fix-font-encoding.sql
-- =============================================================================

-- Common double-encoded sequences (UTF-8 bytes misread as latin1)
-- ┬á  = U+00A0 (non-breaking space) double-encoded
-- ├   = various accented chars

UPDATE mdl_question
SET questiontext = REPLACE(questiontext, '┬á', ' ')
WHERE questiontext LIKE '%┬á%';

UPDATE mdl_question
SET questiontext = REPLACE(questiontext, '┬â', 'Â')
WHERE questiontext LIKE '%┬â%';

UPDATE mdl_question
SET questiontext = REPLACE(questiontext, '├á', 'á')
WHERE questiontext LIKE '%├á%';

UPDATE mdl_question
SET questiontext = REPLACE(questiontext, '├Á', 'Á')
WHERE questiontext LIKE '%├Á%';

UPDATE mdl_question
SET questiontext = REPLACE(questiontext, '├©', 'é')
WHERE questiontext LIKE '%├©%';

UPDATE mdl_question
SET questiontext = REPLACE(questiontext, '├«', 'í')
WHERE questiontext LIKE '%├«%';

UPDATE mdl_question
SET questiontext = REPLACE(questiontext, '├│', 'ó')
WHERE questiontext LIKE '%├│%';

UPDATE mdl_question
SET questiontext = REPLACE(questiontext, '├║', 'ú')
WHERE questiontext LIKE '%├║%';

-- Fix in answer text as well
UPDATE mdl_question_answers
SET answer = REPLACE(answer, '┬á', ' ')
WHERE answer LIKE '%┬á%';

-- Fix in quiz intro / section headings
UPDATE mdl_quiz
SET intro = REPLACE(intro, '┬á', ' ')
WHERE intro LIKE '%┬á%';

-- Fix in course section summaries (e.g. IELTS lesson names)
UPDATE mdl_course_sections
SET summary = REPLACE(summary, '┬á', ' ')
WHERE summary LIKE '%┬á%';

-- Fix in activity names
UPDATE mdl_quiz
SET name = REPLACE(name, '┬á', ' ')
WHERE name LIKE '%┬á%';

-- Verify remaining occurrences after fix
SELECT COUNT(*) AS remaining_in_questions
FROM mdl_question
WHERE questiontext LIKE '%┬á%';

SELECT COUNT(*) AS remaining_in_answers
FROM mdl_question_answers
WHERE answer LIKE '%┬á%';

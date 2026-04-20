#!/bin/bash
echo "=== All external services ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, name, shortname, enabled, restrictedusers FROM mdl_external_services ORDER BY id;"

echo ""
echo "=== Functions in service ID=1 (student3's service) ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT functionname FROM mdl_external_services_functions WHERE externalserviceid=1 ORDER BY functionname;"

echo ""
echo "=== Does service 1 have mod_quiz_start_attempt? ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT COUNT(*) AS has_start_attempt FROM mdl_external_services_functions WHERE externalserviceid=1 AND functionname='mod_quiz_start_attempt';"

echo ""
echo "=== Student3's token details ==="
STUDENT_TOKEN=$(docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -sN -e \
  "SELECT token FROM mdl_external_tokens WHERE userid=4 ORDER BY timecreated DESC LIMIT 1;")
echo "Token: $STUDENT_TOKEN"

echo ""
echo "=== Direct Moodle test: mod_quiz_get_quiz_access_information (student token) ==="
curl -s "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${STUDENT_TOKEN}&wsfunction=mod_quiz_get_quiz_access_information&moodlewsrestformat=json&quizid=2"
echo ""

echo ""
echo "=== Direct Moodle test: mod_quiz_start_attempt (student token) ==="
curl -s -X POST "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${STUDENT_TOKEN}&wsfunction=mod_quiz_start_attempt&moodlewsrestformat=json&quizid=2"
echo ""

echo ""
echo "=== Add mod_quiz_start_attempt to service 1 if missing ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "INSERT IGNORE INTO mdl_external_services_functions (externalserviceid, functionname)
   SELECT 1, fname FROM (
     SELECT 'mod_quiz_start_attempt' AS fname
     UNION SELECT 'mod_quiz_get_attempt_data'
     UNION SELECT 'mod_quiz_save_attempt'
     UNION SELECT 'mod_quiz_process_attempt'
     UNION SELECT 'mod_quiz_get_attempt_review'
     UNION SELECT 'mod_quiz_get_attempt_summary'
     UNION SELECT 'mod_quiz_get_user_attempts'
     UNION SELECT 'mod_quiz_get_quizzes_by_courses'
     UNION SELECT 'mod_quiz_get_quiz_access_information'
     UNION SELECT 'enrol_manual_enrol_users'
   ) t WHERE fname NOT IN (
     SELECT functionname FROM mdl_external_services_functions WHERE externalserviceid=1
   );"
echo "Done adding functions"

echo ""
echo "=== Re-test: mod_quiz_start_attempt (student token) after adding functions ==="
curl -s -X POST "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${STUDENT_TOKEN}&wsfunction=mod_quiz_start_attempt&moodlewsrestformat=json&quizid=2"
echo ""

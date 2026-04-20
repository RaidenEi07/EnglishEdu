#!/bin/bash
echo "=== Check student3 suspended status and auth ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, username, suspended, confirmed, deleted, auth, policyagreed
   FROM mdl_user WHERE username='student3';"

echo ""
echo "=== Check Moodle global settings (web services enabled?) ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT name, value FROM mdl_config
   WHERE name IN ('enablewebservices','enablemobilewebservice','auth');  "

echo ""
echo "=== Check webservice/rest:use capability for student role ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT rc.roleid, rc.capability, rc.permission, r.name
   FROM mdl_role_capabilities rc JOIN mdl_role r ON r.id=rc.roleid
   WHERE rc.capability LIKE '%webservice%';"

echo ""
echo "=== FIX: Unsuspend student3 if suspended ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "UPDATE mdl_user SET suspended=0, policyagreed=1, confirmed=1
   WHERE username='student3';"
echo "Done"

echo ""
echo "=== Verify student3 after fix ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, username, suspended, confirmed, deleted, auth, policyagreed
   FROM mdl_user WHERE username='student3';"

echo ""
echo "=== Re-test: mod_quiz_get_quiz_access_information with student token ==="
STUDENT_TOKEN=$(docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -sN -e \
  "SELECT token FROM mdl_external_tokens WHERE userid=4 ORDER BY timecreated DESC LIMIT 1;")
curl -s "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${STUDENT_TOKEN}&wsfunction=mod_quiz_get_quiz_access_information&moodlewsrestformat=json&quizid=2"
echo ""

echo ""
echo "=== Re-test: mod_quiz_start_attempt with student token ==="
curl -s -X POST "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${STUDENT_TOKEN}&wsfunction=mod_quiz_start_attempt&moodlewsrestformat=json&quizid=2"
echo ""

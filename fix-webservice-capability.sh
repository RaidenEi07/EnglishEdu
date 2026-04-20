#!/bin/bash
echo "=== Find system context ID ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, contextlevel, instanceid FROM mdl_context WHERE contextlevel=10;"

echo ""
echo "=== Check current webservice/rest:use capability for ALL roles ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT rc.roleid, rc.capability, rc.permission, rc.contextid, r.shortname
   FROM mdl_role_capabilities rc JOIN mdl_role r ON r.id=rc.roleid
   WHERE rc.capability IN ('webservice/rest:use','moodle/webservice:createtoken','moodle/webservice:createmobiletoken')
   ORDER BY rc.capability, rc.roleid;"

echo ""
echo "=== Check student3 role assignments in SYSTEM context ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT ra.userid, ra.roleid, ra.contextid, c.contextlevel, r.shortname
   FROM mdl_role_assignments ra 
   JOIN mdl_context c ON c.id=ra.contextid
   JOIN mdl_role r ON r.id=ra.roleid
   WHERE ra.userid=4;"

echo ""
echo "=== FIX 1: Grant webservice/rest:use to student role (roleid=5) at system context ==="
SYS_CTX=$(docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -sN -e \
  "SELECT id FROM mdl_context WHERE contextlevel=10 LIMIT 1;")
echo "System context ID: $SYS_CTX"

docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "INSERT INTO mdl_role_capabilities (contextid, roleid, capability, permission, timemodified, modifierid)
   VALUES ($SYS_CTX, 5, 'webservice/rest:use', 1, UNIX_TIMESTAMP(), 2)
   ON DUPLICATE KEY UPDATE permission=1, timemodified=UNIX_TIMESTAMP();"
echo "Done: webservice/rest:use granted to student role"

echo ""
echo "=== FIX 2: Grant moodle/webservice:createtoken to student role at system context ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "INSERT INTO mdl_role_capabilities (contextid, roleid, capability, permission, timemodified, modifierid)
   VALUES ($SYS_CTX, 5, 'moodle/webservice:createtoken', 1, UNIX_TIMESTAMP(), 2)
   ON DUPLICATE KEY UPDATE permission=1, timemodified=UNIX_TIMESTAMP();"
echo "Done: moodle/webservice:createtoken granted to student role"

echo ""
echo "=== FIX 3: Grant moodle/webservice:createmobiletoken to student role ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "INSERT INTO mdl_role_capabilities (contextid, roleid, capability, permission, timemodified, modifierid)
   VALUES ($SYS_CTX, 5, 'moodle/webservice:createmobiletoken', 1, UNIX_TIMESTAMP(), 2)
   ON DUPLICATE KEY UPDATE permission=1, timemodified=UNIX_TIMESTAMP();"
echo "Done: moodle/webservice:createmobiletoken granted to student role"

echo ""
echo "=== Purge Moodle caches (important!) ==="
docker exec englishedu-moodle-1 php /opt/bitnami/moodle/admin/cli/purge_caches.php 2>&1 || echo "Cache purge might have failed, continuing..."

echo ""
echo "=== Re-test: mod_quiz_get_quiz_access_information with student token ==="
STUDENT_TOKEN=$(docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -sN -e \
  "SELECT token FROM mdl_external_tokens WHERE userid=4 ORDER BY timecreated DESC LIMIT 1;")
echo "Using token: $STUDENT_TOKEN"

curl -s "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${STUDENT_TOKEN}&wsfunction=mod_quiz_get_quiz_access_information&moodlewsrestformat=json&quizid=2"
echo ""

echo ""
echo "=== Re-test: mod_quiz_start_attempt with student token ==="
curl -s -X POST "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${STUDENT_TOKEN}&wsfunction=mod_quiz_start_attempt&moodlewsrestformat=json&quizid=2"
echo ""

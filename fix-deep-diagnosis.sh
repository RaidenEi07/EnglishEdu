#!/bin/bash
set -e

echo "=== Test token DIRECTLY after password reset ==="
TOKEN="83990d32129a9f684c6f77c68f1cb3db"
curl -s "http://221.132.21.13:8080/webservice/rest/server.php?wstoken=${TOKEN}&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json" | python3 -c "import sys,json; d=json.load(sys.stdin); print('SITE INFO OK:', d.get('sitename','?'))" 2>/dev/null || \
curl -s "http://221.132.21.13:8080/webservice/rest/server.php?wstoken=${TOKEN}&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json"
echo ""

echo "=== Check defaultuserroleid config ==="
docker exec englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT name, value FROM mdl_config WHERE name IN ('defaultuserroleid','defaultcourseroleid','guestroleid'); SELECT id, shortname, archetype FROM mdl_role;" 2>/dev/null

echo ""
echo "=== Check mdl_external_tokens directly ==="
docker exec englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, token, userid, externalserviceid, validuntil, contextid FROM mdl_external_tokens WHERE token='83990d32129a9f684c6f77c68f1cb3db';" 2>/dev/null

echo ""
echo "=== Check what login/token.php creates in mdl_external_tokens for student3 ==="
docker exec englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT et.id, et.token, et.userid, et.externalserviceid, et.validuntil, et.contextid, es.shortname FROM mdl_external_tokens et JOIN mdl_external_services es ON es.id=et.externalserviceid WHERE et.userid=4;" 2>/dev/null

echo ""
echo "=== Purge all Moodle caches forcefully ==="
docker exec englishedu-moodle-1 php /opt/bitnami/moodle/admin/cli/purge_caches.php 2>&1
echo "Cache purged"

echo ""
echo "=== Add student3 EXPLICITLY to system context with roleid=7 ==="
docker exec englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle -e \
  "INSERT IGNORE INTO mdl_role_assignments (roleid, contextid, userid, timemodified, modifierid, component, itemid, sortorder)
   VALUES (7, 1, 4, UNIX_TIMESTAMP(), 2, '', 0, 0);" 2>/dev/null
echo "Done: student3 explicitly assigned roleid=7 in system context"

echo ""
echo "=== Check role assignments for student3 now ==="
docker exec englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT ra.userid, ra.roleid, ra.contextid, c.contextlevel, r.shortname FROM mdl_role_assignments ra JOIN mdl_context c ON c.id=ra.contextid JOIN mdl_role r ON r.id=ra.roleid WHERE ra.userid=4;" 2>/dev/null

echo ""
echo "=== Purge caches again after role assignment ==="
docker exec englishedu-moodle-1 php /opt/bitnami/moodle/admin/cli/purge_caches.php 2>&1
echo "Cache purged again"

echo ""
echo "=== FINAL TEST: core_webservice_get_site_info ==="
curl -s "http://221.132.21.13:8080/webservice/rest/server.php?wstoken=${TOKEN}&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json"
echo ""

echo ""
echo "=== FINAL TEST: mod_quiz_get_quiz_access_information ==="
curl -s "http://221.132.21.13:8080/webservice/rest/server.php?wstoken=${TOKEN}&wsfunction=mod_quiz_get_quiz_access_information&quizid=2&moodlewsrestformat=json"
echo ""

echo ""
echo "=== FINAL TEST: mod_quiz_start_attempt ==="
curl -s -X POST "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${TOKEN}&wsfunction=mod_quiz_start_attempt&quizid=2&moodlewsrestformat=json"
echo ""

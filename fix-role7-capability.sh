#!/bin/bash
echo "=== Add webservice/rest:use to the authenticated user role (roleid=7) at system context ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "INSERT INTO mdl_role_capabilities (contextid, roleid, capability, permission, timemodified, modifierid)
   VALUES (1, 7, 'webservice/rest:use', 1, UNIX_TIMESTAMP(), 2)
   ON DUPLICATE KEY UPDATE permission=1, timemodified=UNIX_TIMESTAMP();"
echo "Done: webservice/rest:use granted to authenticated user role (roleid=7)"

echo ""
echo "=== Verify capabilities now ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT rc.roleid, rc.capability, rc.permission, r.shortname
   FROM mdl_role_capabilities rc JOIN mdl_role r ON r.id=rc.roleid
   WHERE rc.capability LIKE 'webservice%'
   ORDER BY rc.capability, rc.roleid;"

echo ""
echo "=== Try login/token.php to get a FRESH token for student3 ==="
FRESH_TOKEN=$(curl -s "http://221.132.21.13:8080/login/token.php?username=student3&password=Student%40123&service=moodle_mobile_app" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('token','ERROR: '+str(d)))" 2>/dev/null)
echo "Fresh token from login/token.php: $FRESH_TOKEN"

echo ""
echo "=== Test with original DB token (after adding webservice/rest:use to role 7) ==="
STUDENT_TOKEN=$(docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -sN -e \
  "SELECT token FROM mdl_external_tokens WHERE userid=4 ORDER BY timecreated DESC LIMIT 1;")
echo "DB token: $STUDENT_TOKEN"
curl -s "http://221.132.21.13:8080/webservice/rest/server.php" \
  -d "wstoken=${STUDENT_TOKEN}&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json"
echo ""

echo ""
echo "=== Test with fresh token (if obtained) ==="
if [ "$FRESH_TOKEN" != "ERROR"* ] && [ -n "$FRESH_TOKEN" ] && [ "$FRESH_TOKEN" != "ERROR" ]; then
  echo "Testing fresh token..."
  curl -s "http://221.132.21.13:8080/webservice/rest/server.php" \
    -d "wstoken=${FRESH_TOKEN}&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json"
  echo ""
  echo ""
  echo "=== mod_quiz_start_attempt with fresh token ==="
  curl -s -X POST "http://221.132.21.13:8080/webservice/rest/server.php" \
    -d "wstoken=${FRESH_TOKEN}&wsfunction=mod_quiz_start_attempt&moodlewsrestformat=json&quizid=2"
  echo ""
else
  echo "Could not get fresh token via login/token.php"
fi

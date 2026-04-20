#!/bin/bash
echo "=== Quiz 2 settings (timeopen, timeclose, password, visible) ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, course, name, visible, timeopen, timeclose, quizpassword, browsersecurity, subnet
   FROM mdl_quiz WHERE id=2;"

echo ""
echo "=== Course 2 visibility ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, shortname, visible, startdate, enddate FROM mdl_course WHERE id=2;"

echo ""
echo "=== Quiz coursemodule visible status ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT cm.id, cm.course, cm.module, cm.instance, cm.visible, cm.visibleoncoursepage
   FROM mdl_course_modules cm
   JOIN mdl_modules m ON m.id=cm.module
   WHERE m.name='quiz' AND cm.instance=2;"

echo ""
echo "=== moodle_mobile_app service - restrictedusers flag ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, name, enabled, restrictedusers, downloadfiles, uploadfiles
   FROM mdl_external_services WHERE name='moodle_mobile_app';"

echo ""
echo "=== Allowed users for service (if restrictedusers=1) ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT esu.id, esu.externalserviceid, esu.userid, u.username
   FROM mdl_external_services_users esu
   JOIN mdl_user u ON u.id=esu.userid
   JOIN mdl_external_services es ON es.id=esu.externalserviceid
   WHERE es.name='moodle_mobile_app';"

echo ""
echo "=== Student3 (moodle_id=4) existing tokens ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, token, userid, externalserviceid, validuntil, timecreated
   FROM mdl_external_tokens WHERE userid=4 ORDER BY timecreated DESC LIMIT 5;"

echo ""
echo "=== Student3 role capabilities for mod/quiz:attempt ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT rc.roleid, rc.capability, rc.permission, r.name AS rolename
   FROM mdl_role_capabilities rc
   JOIN mdl_role r ON r.id=rc.roleid
   WHERE rc.capability='mod/quiz:attempt';"

echo ""
echo "=== Get student3 user token via Moodle REST ==="
TOKEN_RESP=$(curl -s "http://localhost:8080/login/token.php?username=student3&password=Student%40123&service=moodle_mobile_app")
echo "$TOKEN_RESP"
MOODLE_TOKEN=$(echo "$TOKEN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('token','ERROR'))" 2>/dev/null)
echo "Student moodle token: $MOODLE_TOKEN"

if [ "$MOODLE_TOKEN" != "ERROR" ] && [ -n "$MOODLE_TOKEN" ]; then
  echo ""
  echo "=== Test mod_quiz_get_quiz_access_information (user token) ==="
  curl -s "http://localhost:8080/webservice/rest/server.php?wstoken=${MOODLE_TOKEN}&wsfunction=mod_quiz_get_quiz_access_information&moodlewsrestformat=json&quizid=2"
  echo ""
  echo ""
  echo "=== Test mod_quiz_start_attempt directly via user token ==="
  curl -s -X POST "http://localhost:8080/webservice/rest/server.php" \
    -d "wstoken=${MOODLE_TOKEN}&wsfunction=mod_quiz_start_attempt&moodlewsrestformat=json&quizid=2"
  echo ""
fi

echo ""
echo "=== Quiz access rules for quiz 2 ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT * FROM mdl_quizaccess_timelimit WHERE quizid=2;" 2>/dev/null || echo "(no timelimit table)"
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT name FROM mdl_tables WHERE name LIKE 'mdl_quizaccess%';" 2>/dev/null || true

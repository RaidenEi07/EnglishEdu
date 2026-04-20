#!/bin/bash
set -e

echo "=== 1. Check if enrol_manual_enrol_users is in moodle_mobile_app service ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT esf.functionname FROM mdl_external_services_functions esf
   JOIN mdl_external_services es ON esf.externalserviceid=es.id
   WHERE es.name='moodle_mobile_app' AND esf.functionname LIKE '%enrol%';"

echo ""
echo "=== 2. Check student3 enrollment in Moodle courses ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT u.username, u.id as moodle_uid, ue.status, e.courseid, e.enrol
   FROM mdl_user_enrolments ue
   JOIN mdl_enrol e ON ue.enrolid=e.id
   JOIN mdl_user u ON ue.userid=u.id
   WHERE u.username='student3';"

echo ""
echo "=== 3. Check mdl_enrol (manual) for course 2 ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT id, courseid, enrol, status FROM mdl_enrol WHERE courseid=2;"

echo ""
echo "=== 4. Add enrol_manual_enrol_users to service if missing ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "INSERT IGNORE INTO mdl_external_services_functions (externalserviceid, functionname)
   SELECT es.id, 'enrol_manual_enrol_users'
   FROM mdl_external_services es
   WHERE es.name='moodle_mobile_app'
   AND NOT EXISTS (
     SELECT 1 FROM mdl_external_services_functions esf2
     WHERE esf2.externalserviceid=es.id AND esf2.functionname='enrol_manual_enrol_users'
   );"
echo "Done (0 rows = already existed)"

echo ""
echo "=== 5. Directly enroll student3 (moodle_id=4) in Moodle course 2 via DB ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SET @courseid=2; SET @userid=4;
   -- Ensure manual enrol method exists for course
   INSERT IGNORE INTO mdl_enrol (enrol, status, courseid, sortorder, timecreated, timemodified)
   SELECT 'manual', 0, @courseid, COALESCE((SELECT MAX(sortorder)+1 FROM mdl_enrol e2 WHERE e2.courseid=@courseid),0), UNIX_TIMESTAMP(), UNIX_TIMESTAMP()
   WHERE NOT EXISTS (SELECT 1 FROM mdl_enrol e3 WHERE e3.courseid=@courseid AND e3.enrol='manual');
   -- Insert user enrolment
   INSERT INTO mdl_user_enrolments (status, enrolid, userid, timestart, timeend, timecreated, timemodified, modifierid)
   SELECT 0, e.id, @userid, UNIX_TIMESTAMP()-86400, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), 2
   FROM mdl_enrol e WHERE e.courseid=@courseid AND e.enrol='manual'
   ON DUPLICATE KEY UPDATE status=0, timemodified=UNIX_TIMESTAMP();"

echo ""
echo "=== 6. Assign student role (roleid=5) in course context ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SET @courseid=2; SET @userid=4;
   SET @contextid=(SELECT id FROM mdl_context WHERE contextlevel=50 AND instanceid=@courseid);
   INSERT IGNORE INTO mdl_role_assignments (roleid, contextid, userid, timemodified, modifierid, component, itemid, sortorder)
   VALUES (5, @contextid, @userid, UNIX_TIMESTAMP(), 2, '', 0, 0);"

echo ""
echo "=== 7. Verify enrollment ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle -e \
  "SELECT u.username, ue.status, e.courseid, e.enrol, ra.roleid
   FROM mdl_user_enrolments ue
   JOIN mdl_enrol e ON ue.enrolid=e.id
   JOIN mdl_user u ON ue.userid=u.id
   LEFT JOIN mdl_context ctx ON ctx.contextlevel=50 AND ctx.instanceid=e.courseid
   LEFT JOIN mdl_role_assignments ra ON ra.userid=u.id AND ra.contextid=ctx.id
   WHERE u.username='student3' AND e.courseid=2;"

echo ""
echo "=== 8. Now test quiz/start ==="
TOKEN=$(curl -s -X POST http://localhost/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student3","password":"Student@123"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))")
echo "Token obtained: ${TOKEN:0:20}..."

echo ""
echo "--- POST /moodle/quiz/start?quizId=2 ---"
START_RESP=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost/api/v1/moodle/quiz/start?quizId=2")
echo "$START_RESP"

ATTEMPT_ID=$(echo "$START_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('attempt',{}).get('id',''))" 2>/dev/null || echo "")
echo "AttemptId=$ATTEMPT_ID"

if [ -n "$ATTEMPT_ID" ] && [ "$ATTEMPT_ID" != "None" ] && [ "$ATTEMPT_ID" != "" ]; then
  echo ""
  echo "--- GET /moodle/quiz/attempt-data?attemptId=${ATTEMPT_ID}&page=0 ---"
  curl -s -H "Authorization: Bearer $TOKEN" \
    "http://localhost/api/v1/moodle/quiz/attempt-data?attemptId=${ATTEMPT_ID}&page=0" | head -c 400
  echo ""

  echo ""
  echo "--- GET /moodle/quiz/summary?attemptId=${ATTEMPT_ID} ---"
  curl -s -H "Authorization: Bearer $TOKEN" \
    "http://localhost/api/v1/moodle/quiz/summary?attemptId=${ATTEMPT_ID}" | head -c 400
  echo ""

  echo ""
  echo "SUCCESS: Full quiz flow working!"
else
  echo ""
  echo "STILL FAILING - checking backend logs..."
  docker logs --tail 30 englishedu-backend-1 2>&1 | grep -E "quiz|enrol|ERROR|WARN|Exception" | tail -20
fi

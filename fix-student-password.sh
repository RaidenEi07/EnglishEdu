#!/bin/bash
echo "=== Token details for student3 ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle 2>/dev/null <<'EOF'
SELECT et.id, et.token, et.userid, et.externalserviceid, et.validuntil,
       et.iprestriction, et.contextid, 
       es.name, es.shortname, es.enabled, es.restrictedusers,
       UNIX_TIMESTAMP() as now_ts
FROM mdl_external_tokens et
JOIN mdl_external_services es ON es.id=et.externalserviceid
WHERE et.userid=4;
EOF

echo ""
echo "=== Check contextid for this token vs site context ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle 2>/dev/null <<'EOF'
SELECT c.id, c.contextlevel, c.instanceid FROM mdl_context c WHERE c.contextlevel=10;
EOF

echo ""
echo "=== Is the user auth type valid for webservice? ==="
docker exec englishedu-mariadb-1 mysql -u bn_moodle -pbn_pass bitnami_moodle 2>/dev/null <<'EOF'
SELECT u.id, u.username, u.auth, u.suspended, u.confirmed,
       p.value as hash
FROM mdl_user u
LEFT JOIN mdl_user_preferences p ON p.userid=u.id AND p.name='auth_forcepasswordchange'
WHERE u.username='student3';
EOF

echo ""
echo "=== Reset student3 password via Moodle CLI ==="
docker exec englishedu-moodle-1 php /opt/bitnami/moodle/admin/cli/reset_password.php --username=student3 --password=Student@123 2>&1

echo ""
echo "=== Try login/token.php again with reset password ==="
curl -sL "http://221.132.21.13:8080/login/token.php?username=student3&password=Student%40123&service=moodle_mobile_app"
echo ""

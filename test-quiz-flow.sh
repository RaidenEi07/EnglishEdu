#!/bin/bash
set -e

BASE=http://localhost/api/v1

echo "=== Login ==="
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student3","password":"Student@123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token: ${TOKEN:0:30}..."

echo ""
echo "=== 1. GET quiz/attempts (before start) ==="
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/moodle/quiz/attempts?quizId=2"
echo ""

echo ""
echo "=== 2. POST quiz/start ==="
START_RESP=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/moodle/quiz/start?quizId=2")
echo "$START_RESP"
ATTEMPT_ID=$(echo "$START_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "AttemptId=$ATTEMPT_ID"

if [ -z "$ATTEMPT_ID" ]; then
  echo "ERROR: No attempt ID returned, aborting."
  exit 1
fi

echo ""
echo "=== 3. GET quiz/attempt-data (page 0) ==="
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/moodle/quiz/attempt-data?attemptId=${ATTEMPT_ID}&page=0" | head -c 500
echo ""

echo ""
echo "=== 4. GET quiz/summary ==="
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/moodle/quiz/summary?attemptId=${ATTEMPT_ID}" | head -c 300
echo ""

echo ""
echo "=== 5. GET quiz/attempts (after start) ==="
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/moodle/quiz/attempts?quizId=2"
echo ""

echo ""
echo "=== All done ==="

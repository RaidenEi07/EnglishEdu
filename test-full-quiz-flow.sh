#!/bin/bash
TOKEN="83990d32129a9f684c6f77c68f1cb3db"
BASE="http://221.132.21.13:8080/webservice/rest/server.php"

echo "=== 1. Get existing attempts for quiz 2 ==="
ATTEMPTS=$(curl -s "${BASE}?wstoken=${TOKEN}&wsfunction=mod_quiz_get_user_attempts&moodlewsrestformat=json&quizid=2&status=unfinished")
echo "$ATTEMPTS" | python3 -m json.tool 2>/dev/null || echo "$ATTEMPTS"

# Extract attempt ID
ATTEMPT_ID=$(echo "$ATTEMPTS" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['attempts'][0]['id'])" 2>/dev/null)
echo ""
echo "Attempt ID: $ATTEMPT_ID"

echo ""
echo "=== 2. Get attempt data (page 0) ==="
ATTEMPT_DATA=$(curl -s "${BASE}?wstoken=${TOKEN}&wsfunction=mod_quiz_get_attempt_data&moodlewsrestformat=json&attemptid=${ATTEMPT_ID}&page=0")
echo "$ATTEMPT_DATA" | python3 -m json.tool 2>/dev/null || echo "$ATTEMPT_DATA"

echo ""
echo "=== 3. Get attempt summary ==="
SUMMARY=$(curl -s "${BASE}?wstoken=${TOKEN}&wsfunction=mod_quiz_get_attempt_summary&moodlewsrestformat=json&attemptid=${ATTEMPT_ID}")
echo "$SUMMARY" | python3 -m json.tool 2>/dev/null || echo "$SUMMARY"

echo ""
echo "=== 4. Process attempt (finish it) ==="
PROCESS=$(curl -s -X POST "${BASE}" -d "wstoken=${TOKEN}&wsfunction=mod_quiz_process_attempt&moodlewsrestformat=json&attemptid=${ATTEMPT_ID}&finishattempt=1")
echo "$PROCESS" | python3 -m json.tool 2>/dev/null || echo "$PROCESS"

echo ""
echo "=== 5. Get attempt review ==="
REVIEW=$(curl -s "${BASE}?wstoken=${TOKEN}&wsfunction=mod_quiz_get_attempt_review&moodlewsrestformat=json&attemptid=${ATTEMPT_ID}")
echo "$REVIEW" | python3 -c "import sys,json; d=json.load(sys.stdin); print('State:', d.get('attempt',{}).get('state')); print('Grade:', d.get('grade')); print('Questions:', len(d.get('questions',[])))" 2>/dev/null || echo "$REVIEW" | head -c 500

echo ""
echo "=== 6. Start a NEW attempt ==="
NEW_ATTEMPT=$(curl -s -X POST "${BASE}" -d "wstoken=${TOKEN}&wsfunction=mod_quiz_start_attempt&moodlewsrestformat=json&quizid=2")
echo "$NEW_ATTEMPT" | python3 -c "import sys,json; d=json.load(sys.stdin); print('New attempt ID:', d.get('attempt',{}).get('id')); print('State:', d.get('attempt',{}).get('state'))" 2>/dev/null || echo "$NEW_ATTEMPT" | head -c 500

echo ""
echo "=== DONE ==="

#!/bin/bash
RESP=$(curl -sf -X POST http://localhost/api/v1/auth/login -H 'Content-Type: application/json' -d '{"username":"student3","password":"Student@123"}')
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null)
echo "Token len: ${#TOKEN}"

echo "=== POST /moodle/quiz/start?quizId=2 ==="
R=$(curl -s -X POST "http://localhost/api/v1/moodle/quiz/start?quizId=2" -H "Authorization: Bearer $TOKEN")
echo "RAW: $(echo $R | head -c 300)"
echo "$R" | python3 -c "import sys,json; d=json.load(sys.stdin); a=d.get('data',{}).get('attempt',{}); print('success:',d.get('success')); print('msg:',d.get('message','')); print('attempt_id:',a.get('id')); print('state:',a.get('state'))" 2>/dev/null
ATTEMPT_ID=$(echo "$R" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('attempt',{}).get('id',''))" 2>/dev/null)

if [ -z "$ATTEMPT_ID" ] || [ "$ATTEMPT_ID" = "None" ]; then
  echo "No attempt returned from start, fetching in-progress..."
  R2=$(curl -s "http://localhost/api/v1/moodle/quiz/attempts?quizId=2" -H "Authorization: Bearer $TOKEN")
  ATTEMPT_ID=$(echo "$R2" | python3 -c "import sys,json; d=json.load(sys.stdin); a=[x for x in d.get('data',{}).get('attempts',[]) if x.get('state')=='inprogress']; print(a[0]['id'] if a else '')" 2>/dev/null)
fi
echo "Using attempt ID: $ATTEMPT_ID"

echo ""
echo "=== GET /moodle/quiz/attempt-data?attemptId=$ATTEMPT_ID&page=0 ==="
curl -s "http://localhost/api/v1/moodle/quiz/attempt-data?attemptId=$ATTEMPT_ID&page=0" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json; d=json.load(sys.stdin); print('success:',d.get('success')); print('questions:',len(d.get('data',{}).get('questions',[])))" 2>/dev/null

echo ""
echo "=== GET /moodle/quiz/summary?attemptId=$ATTEMPT_ID ==="
curl -s "http://localhost/api/v1/moodle/quiz/summary?attemptId=$ATTEMPT_ID" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json; d=json.load(sys.stdin); print('success:',d.get('success')); print('unanswered:',d.get('data',{}).get('totalunanswered'))" 2>/dev/null

echo ""
echo "=== POST /moodle/quiz/submit (JSON body with attemptId) ==="
SUBMIT_BODY="{\"attemptId\": $ATTEMPT_ID, \"answers\": {}}"
SUBMIT_R=$(curl -s -X POST "http://localhost/api/v1/moodle/quiz/submit" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$SUBMIT_BODY")
echo "RAW: $(echo $SUBMIT_R | head -c 300)"
echo "$SUBMIT_R" | python3 -c "import sys,json; d=json.load(sys.stdin); print('success:',d.get('success')); print('state:',d.get('data',{}).get('state'))" 2>/dev/null

echo ""
echo "=== GET /moodle/quiz/review?attemptId=$ATTEMPT_ID ==="
curl -s "http://localhost/api/v1/moodle/quiz/review?attemptId=$ATTEMPT_ID" -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json; d=json.load(sys.stdin); print('success:',d.get('success')); a=d.get('data',{}).get('attempt',{}); print('state:',a.get('state')); print('grade:',d.get('data',{}).get('grade'))" 2>/dev/null

echo ""
echo "=== DONE ==="

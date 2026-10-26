#!/usr/bin/env bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8085}"
echo "Running smoke tests against $BASE_URL ..."

# 1. Check health / public config
echo "1. Testing /api/public/config..."
CONFIG_RESP=$(curl -s "$BASE_URL/api/public/config")
echo "Config: $CONFIG_RESP"

# 2. Login as Demo Employee
echo "2. Logging in as Employee..."
EMP_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"employee@nimbusfinserve.com","password":"Password123!"}')
EMP_TOKEN=$(echo "$EMP_LOGIN" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$EMP_TOKEN" ]; then
  echo "FAIL: Could not log in as employee"
  exit 1
fi
echo "Employee login OK"

# 3. Login as Admin
echo "3. Logging in as Administrator..."
ADMIN_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@nimbusfinserve.com","password":"Password123!"}')
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "FAIL: Could not log in as admin"
  exit 1
fi
echo "Admin login OK"

# 4. Create an incident as employee
echo "4. Creating incident as employee..."
INC_RESP=$(curl -s -X POST "$BASE_URL/api/employee/incidents" \
  -H "Authorization: Bearer $EMP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Smoke Test Incident","description":"Testing end-to-end incident submission in Docker Compose"}')
INC_ID=$(echo "$INC_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

if [ -z "$INC_ID" ]; then
  echo "FAIL: Could not create incident"
  exit 1
fi
echo "Incident created OK: $INC_ID"

# 5. Verify technician roster does not leak passwordHash
echo "5. Checking technician roster for password hash leaks..."
ROSTER_RESP=$(curl -s "$BASE_URL/api/admin/technicians" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$ROSTER_RESP" | grep -q "passwordHash"; then
  echo "FAIL: passwordHash leaked in roster"
  exit 1
fi
echo "Roster check OK (no passwordHash)"

echo "=== ALL SMOKE TESTS PASSED SUCCESSFULLY ==="


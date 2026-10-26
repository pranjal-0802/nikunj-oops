@echo off
setlocal

set BASE_URL=http://localhost:8085
echo Running smoke tests against %BASE_URL% ...

echo 1. Testing /api/public/config...
curl -s %BASE_URL%/api/public/config
echo.

echo 2. Testing employee login...
curl -s -X POST %BASE_URL%/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"employee@nimbusfinserve.com\",\"password\":\"Password123!\"}"
echo.

echo 3. Testing admin login...
curl -s -X POST %BASE_URL%/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@nimbusfinserve.com\",\"password\":\"Password123!\"}"
echo.

echo Smoke test complete.


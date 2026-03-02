#!/usr/bin/env bash
set -euo pipefail

# Test registration/login against a native GraalVM image.
# Usage: ./test-native.sh [--skip-build]
#
# This builds the Docker image (same Dockerfile as CI), starts it
# with a Postgres container, generates JWT keys, and runs the
# native integration tests against it.

SKIP_BUILD=false
if [[ "${1:-}" == "--skip-build" ]]; then
  SKIP_BUILD=true
fi

NETWORK="native-test-net"
PG_CONTAINER="native-test-pg"
APP_CONTAINER="native-test-app"
IMAGE="event-planner-native-test"
KEYS_DIR="$(mktemp -d)"

cleanup() {
  echo "Cleaning up..."
  docker rm -f "$APP_CONTAINER" "$PG_CONTAINER" 2>/dev/null || true
  docker network rm "$NETWORK" 2>/dev/null || true
  rm -rf "$KEYS_DIR"
}
trap cleanup EXIT

# Generate ephemeral JWT keys
echo "==> Generating JWT keys..."
openssl genrsa -out "$KEYS_DIR/privateKey.pem" 2048 2>/dev/null
openssl rsa -in "$KEYS_DIR/privateKey.pem" -pubout -out "$KEYS_DIR/publicKey.pem" 2>/dev/null
chmod 644 "$KEYS_DIR/privateKey.pem" "$KEYS_DIR/publicKey.pem"

# Copy keys to src/main/resources for the Docker build (needed on classpath)
cp "$KEYS_DIR/privateKey.pem" src/main/resources/privateKey.pem
cp "$KEYS_DIR/publicKey.pem" src/main/resources/publicKey.pem

# Build native Docker image
if [[ "$SKIP_BUILD" == false ]]; then
  echo "==> Building native Docker image (this takes 5-15 minutes)..."
  docker build -f Dockerfile -t "$IMAGE" .
fi

# Create network
docker network create "$NETWORK" 2>/dev/null || true

# Start Postgres
echo "==> Starting Postgres..."
docker rm -f "$PG_CONTAINER" 2>/dev/null || true
docker run -d \
  --name "$PG_CONTAINER" \
  --network "$NETWORK" \
  -e POSTGRES_DB=event_planner_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:17-alpine

echo "==> Waiting for Postgres..."
for i in $(seq 1 30); do
  if docker exec "$PG_CONTAINER" pg_isready -U postgres &>/dev/null; then
    break
  fi
  sleep 1
done

# Start the native app
echo "==> Starting native application..."
docker rm -f "$APP_CONTAINER" 2>/dev/null || true
docker run -d \
  --name "$APP_CONTAINER" \
  --network "$NETWORK" \
  -p 8090:8080 \
  -v "$KEYS_DIR:/keys:ro" \
  -e DB_URL="jdbc:postgresql://$PG_CONTAINER:5432/event_planner_db" \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e JWT_PRIVATE_KEY_PATH="file:///keys/privateKey.pem" \
  -e JWT_PUBLIC_KEY_PATH="file:///keys/publicKey.pem" \
  -e QUARKUS_DATASOURCE_DEVSERVICES_ENABLED=false \
  -e QUARKUS_FLYWAY_MIGRATE_AT_START=true \
  -e CORS_ORIGINS="http://localhost:8090" \
  -e QUARKUS_PROFILE=prod \
  "$IMAGE"

echo "==> Waiting for application..."
for i in $(seq 1 60); do
  if curl -so /dev/null http://localhost:8090/api/events 2>/dev/null; then
    echo "    Application is ready!"
    break
  fi
  if [[ $i -eq 60 ]]; then
    echo "    ERROR: Application failed to start. Container logs:"
    docker logs "$APP_CONTAINER" 2>&1 | tail -50
    exit 1
  fi
  sleep 1
done

# Run tests
echo ""
echo "==> Running auth tests against native image..."
echo ""

FAILURES=0
BASE="http://localhost:8090"

# Test 1: Register
echo "--- Test: Register creates user and returns token"
EMAIL="native-$(uuidgen)@example.com"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"displayName\":\"Native Test\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [[ "$HTTP_CODE" == "201" ]]; then
  TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)
  RESP_EMAIL=$(echo "$BODY" | grep -o '"email":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [[ -n "$TOKEN" && "$RESP_EMAIL" == "$EMAIL" ]]; then
    echo "    PASS"
  else
    echo "    FAIL: token or email missing in response: $BODY"
    FAILURES=$((FAILURES + 1))
  fi
else
  echo "    FAIL: expected 201, got $HTTP_CODE"
  echo "    Body: $BODY"
  FAILURES=$((FAILURES + 1))
fi

# Test 2: Login
echo "--- Test: Login with valid credentials returns token"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"password123\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [[ "$HTTP_CODE" == "200" ]]; then
  LOGIN_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [[ -n "$LOGIN_TOKEN" ]]; then
    echo "    PASS"
  else
    echo "    FAIL: no token in response: $BODY"
    FAILURES=$((FAILURES + 1))
  fi
else
  echo "    FAIL: expected 200, got $HTTP_CODE"
  echo "    Body: $BODY"
  FAILURES=$((FAILURES + 1))
fi

# Test 3: GET /api/users/me with register token
echo "--- Test: Token from register works for /api/users/me"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE/api/users/me" \
  -H "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [[ "$HTTP_CODE" == "200" ]]; then
  RESP_EMAIL=$(echo "$BODY" | grep -o '"email":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [[ "$RESP_EMAIL" == "$EMAIL" ]]; then
    echo "    PASS"
  else
    echo "    FAIL: email mismatch: $BODY"
    FAILURES=$((FAILURES + 1))
  fi
else
  echo "    FAIL: expected 200, got $HTTP_CODE"
  echo "    Body: $BODY"
  FAILURES=$((FAILURES + 1))
fi

# Test 4: GET /api/users/me with login token
echo "--- Test: Token from login works for /api/users/me"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE/api/users/me" \
  -H "Authorization: Bearer $LOGIN_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [[ "$HTTP_CODE" == "200" ]]; then
  RESP_EMAIL=$(echo "$BODY" | grep -o '"email":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [[ "$RESP_EMAIL" == "$EMAIL" ]]; then
    echo "    PASS"
  else
    echo "    FAIL: email mismatch: $BODY"
    FAILURES=$((FAILURES + 1))
  fi
else
  echo "    FAIL: expected 200, got $HTTP_CODE"
  echo "    Body: $BODY"
  FAILURES=$((FAILURES + 1))
fi

# Test 5: Login with wrong password
echo "--- Test: Login with wrong password returns 401"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"wrongpassword\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
if [[ "$HTTP_CODE" == "401" ]]; then
  echo "    PASS"
else
  BODY=$(echo "$RESPONSE" | sed '$d')
  echo "    FAIL: expected 401, got $HTTP_CODE"
  echo "    Body: $BODY"
  FAILURES=$((FAILURES + 1))
fi

echo ""
if [[ $FAILURES -eq 0 ]]; then
  echo "All 5 tests PASSED"
else
  echo "$FAILURES test(s) FAILED"
  echo ""
  echo "Container logs:"
  docker logs "$APP_CONTAINER" 2>&1 | tail -30
  exit 1
fi

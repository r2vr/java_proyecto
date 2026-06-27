#!/usr/bin/env bash
# End-to-end smoke test for OrderFlow.
#
# Exercises the whole API surface against a running instance: public routes,
# JWT security, edge validation, the order lifecycle (DRAFT -> CONFIRMED),
# caching, the transactional outbox and observability.
#
# Usage:
#   ./scripts/e2e.sh                         # against http://localhost:8080
#   BASE=https://orderflow-xxxx.onrender.com ./scripts/e2e.sh   # against prod
#
# The outbox section (9) inspects the Postgres container directly; it is skipped
# automatically when those containers aren't reachable (e.g. against a remote URL).
#
# Requires: curl, jq. (uuidgen is used if present; falls back otherwise.)
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-proyectojava-db-1}"
APP_CONTAINER="${APP_CONTAINER:-proyectojava-app-1}"
USER_NAME="${USER_NAME:-demo}"
PASSWORD="${PASSWORD:-demo123}"

pass=0; fail=0
ok()   { printf "  \033[32mPASS\033[0m %s\n" "$1"; pass=$((pass+1)); }
ko()   { printf "  \033[31mFAIL\033[0m %s\n" "$1"; fail=$((fail+1)); }
hdr()  { printf "\n\033[1m%s\033[0m\n" "$1"; }
check(){ [ "$2" = "$3" ] && ok "$1 (got $2)" || ko "$1 (got $2, expected $3)"; }
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
uid()  { if command -v uuidgen >/dev/null; then uuidgen | tr 'A-Z' 'a-z'; else cat /proc/sys/kernel/random/uuid; fi; }

echo "Target: $BASE"
echo "(on Render's free tier the first request may take ~50s while the app wakes up)"

hdr "0) Health & docs (public routes)"
check "GET /actuator/health -> 200"        "$(code "$BASE/actuator/health")" 200
check "GET /v3/api-docs -> 200"            "$(code "$BASE/v3/api-docs")" 200
check "GET /swagger-ui/index.html -> 200"  "$(code "$BASE/swagger-ui/index.html")" 200

hdr "1) Security: protected without a token"
check "POST /api/orders no token -> 401"   "$(code -X POST "$BASE/api/orders" -H 'Content-Type: application/json' -d '{}')" 401
check "GET /api/orders no token -> 401"     "$(code "$BASE/api/orders")" 401

hdr "2) Login (JWT issuance)"
check "login wrong creds -> 401"           "$(code -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d "{\"username\":\"$USER_NAME\",\"password\":\"WRONG\"}")" 401
TOKEN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_NAME\",\"password\":\"$PASSWORD\"}" | jq -r '.token // empty')
if [ -n "$TOKEN" ]; then ok "login $USER_NAME returns a token (${TOKEN:0:20}...)"; else ko "login $USER_NAME returned no token"; fi
AUTH=(-H "Authorization: Bearer $TOKEN")

hdr "3) Edge validation (Bean Validation -> 400)"
check "4-letter currency -> 400"           "$(code -X POST "$BASE/api/orders" "${AUTH[@]}" -H 'Content-Type: application/json' -d '{"customerId":"c1","currency":"EURO","lines":[{"productId":"p1","sku":"s1","unitPrice":10,"quantity":1}]}')" 400
check "no lines -> 400"                     "$(code -X POST "$BASE/api/orders" "${AUTH[@]}" -H 'Content-Type: application/json' -d '{"customerId":"c1","currency":"EUR","lines":[]}')" 400
check "negative quantity -> 400"           "$(code -X POST "$BASE/api/orders" "${AUTH[@]}" -H 'Content-Type: application/json' -d '{"customerId":"c1","currency":"EUR","lines":[{"productId":"p1","sku":"s1","unitPrice":10,"quantity":-2}]}')" 400

hdr "4) Create order (201 + Location + body)"
# customerId and productId are typed UUIDs in the domain (avoids primitive obsession).
CUST=$(uid); PROD1=$(uid); PROD2=$(uid)
CREATE_BODY="{\"customerId\":\"$CUST\",\"currency\":\"EUR\",\"lines\":[
  {\"productId\":\"$PROD1\",\"sku\":\"SKU-1\",\"unitPrice\":19.99,\"quantity\":2},
  {\"productId\":\"$PROD2\",\"sku\":\"SKU-2\",\"unitPrice\":5.50,\"quantity\":1}]}"
RESP=$(curl -s -D - -o /tmp/of_body.json -X POST "$BASE/api/orders" "${AUTH[@]}" -H 'Content-Type: application/json' -d "$CREATE_BODY")
STATUS=$(printf '%s' "$RESP" | grep -i '^HTTP' | tail -1 | awk '{print $2}')
LOCATION=$(printf '%s' "$RESP" | grep -i '^location:' | tr -d '\r' | awk '{print $2}')
OID=$(jq -r '.orderId' /tmp/of_body.json)
check "POST /api/orders -> 201" "$STATUS" 201
[ -n "$LOCATION" ] && ok "Location header: $LOCATION" || ko "missing Location header"
[ -n "$OID" ] && [ "$OID" != "null" ] && ok "orderId returned: $OID" || ko "no orderId returned"

hdr "5) Read order (initial DRAFT) + domain-computed total"
GET1=$(curl -s "$BASE/api/orders/$OID" "${AUTH[@]}")
echo "  $(echo "$GET1" | jq -c '{orderId,customerId,currency,status,total,lines:(.lines|length)}')"
check "initial status -> DRAFT" "$(echo "$GET1" | jq -r '.status')" DRAFT
check "total = 19.99*2 + 5.50 = 45.48" "$(echo "$GET1" | jq -r '.total')" 45.48

hdr "6) Confirm order (DRAFT -> CONFIRMED) + cache eviction + invariant"
CONF=$(curl -s -X POST "$BASE/api/orders/$OID/confirm" "${AUTH[@]}")
check "after confirm -> CONFIRMED" "$(echo "$CONF" | jq -r '.status')" CONFIRMED
GET2=$(curl -s "$BASE/api/orders/$OID" "${AUTH[@]}")
check "GET after confirm reflects CONFIRMED (cache evicted)" "$(echo "$GET2" | jq -r '.status')" CONFIRMED
check "re-confirm -> 409 (domain invariant)" "$(code -X POST "$BASE/api/orders/$OID/confirm" "${AUTH[@]}")" 409

hdr "7) Listing and status filter"
check "GET /api/orders -> 200" "$(code "$BASE/api/orders" "${AUTH[@]}")" 200
INCONF=$(curl -s "$BASE/api/orders?status=CONFIRMED" "${AUTH[@]}" | jq -r --arg id "$OID" '[.content[]?|select(.orderId==$id)]|length')
check "status=CONFIRMED includes the order" "$INCONF" 1
INDRAFT=$(curl -s "$BASE/api/orders?status=DRAFT" "${AUTH[@]}" | jq -r --arg id "$OID" '[.content[]?|select(.orderId==$id)]|length')
check "status=DRAFT does NOT include the order" "$INDRAFT" 0

hdr "8) Errors: malformed vs non-existent id"
check "malformed id -> 400" "$(code "$BASE/api/orders/not-a-uuid" "${AUTH[@]}")" 400
check "unknown uuid -> 404" "$(code "$BASE/api/orders/00000000-0000-0000-0000-000000000000" "${AUTH[@]}")" 404

hdr "9) Transactional outbox (events stored & relayed)"
if docker exec "$DB_CONTAINER" true >/dev/null 2>&1; then
  sleep 3  # let the OutboxRelay poll (every ~2s)
  echo "  Outbox rows for this order (type | published):"
  docker exec -i "$DB_CONTAINER" psql -U orderflow -d orderflow -t \
    -c "SELECT type, published FROM outbox WHERE aggregate_id='$OID' ORDER BY created_at;" 2>/dev/null | sed '/^$/d;s/^/    /'
  UNPUB=$(docker exec -i "$DB_CONTAINER" psql -U orderflow -d orderflow -t -A -c "SELECT count(*) FROM outbox WHERE published=false;" 2>/dev/null)
  check "no unpublished events left" "${UNPUB:-NA}" 0
else
  echo "  (skipped: Postgres container '$DB_CONTAINER' not reachable — running against a remote target)"
fi

hdr "10) Observability (Actuator)"
check "GET /actuator/metrics -> 200"    "$(code "$BASE/actuator/metrics" "${AUTH[@]}")" 200
check "GET /actuator/prometheus -> 200" "$(code "$BASE/actuator/prometheus" "${AUTH[@]}")" 200

printf "\n\033[1mRESULT: %d passed, %d failed\033[0m\n" "$pass" "$fail"
exit $([ "$fail" -eq 0 ] && echo 0 || echo 1)

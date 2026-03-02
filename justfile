# Meet - Online Meeting Planner

default:
    @just --list

# Start both backend and frontend in dev mode
dev:
    #!/usr/bin/env bash
    trap 'kill 0' EXIT
    just dev-backend &
    echo "Waiting for backend on localhost:8080..."
    until curl -sf http://localhost:8080/api/events > /dev/null 2>&1; do
        sleep 1
    done
    echo "Backend ready, starting frontend..."
    just dev-frontend &
    wait

# Start backend in dev mode
dev-backend:
    cd backend && ./gradlew quarkusDev

# Start frontend dev server
dev-frontend:
    cd frontend && npm run dev

# Build everything
build:
    cd backend && ./gradlew build -x test
    cd frontend && npm run build

# Run all tests
test:
    cd backend && ./gradlew test
    cd frontend && npx vitest run

# Format and lint all code
fix:
    cd frontend && npx prettier --write .
    cd e2e && npx prettier --write .
    cd backend && ./gradlew ktlintFormat
    cd frontend && npx eslint --fix --no-error-on-unmatched-pattern src/ tests/
    cd e2e && npx eslint --fix tests/ *.ts
    cd backend && ./gradlew ktlintCheck

# Type check frontend and e2e
check:
    cd frontend && npm run check
    cd e2e && npx tsc --noEmit

# Run jOOQ code generation
jooq-codegen:
    cd backend && ./gradlew jooqCodegen

# Start/stop dev database
db:
    docker compose -f docker-compose.dev.yml up -d

db-down:
    docker compose -f docker-compose.dev.yml down

# Run E2E tests
test-e2e:
    cd e2e && npx playwright test

test-e2e-ui:
    cd e2e && npx playwright test --ui

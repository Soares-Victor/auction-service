# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
GRADLE     := ./gradlew
APP_JAR    := build/libs/auction-service-0.0.1-SNAPSHOT.jar

# k6 parameters (override on the CLI: make test AUCTIONS=5 RPS_PER_AUCTION=50)
AUCTIONS          ?= 10
RPS_PER_AUCTION   ?= 100
DURATION          ?= 10m
BASE_URL          ?= http://localhost:8080

# ---------------------------------------------------------------------------
# run — start infra + build + launch backend
# ---------------------------------------------------------------------------
.PHONY: run
run:
	@echo ">>> Stopping any existing auction-service process..."
	-pkill -f "auction-service.*\.jar" 2>/dev/null || true
	@sleep 1
	@echo ">>> Starting infrastructure..."
	docker compose up -d
	@echo ">>> Waiting for Postgres and Kafka to be healthy..."
	@until docker compose exec -T postgres pg_isready -U auction -d auction_db > /dev/null 2>&1; do sleep 2; done
	@echo ">>> Building application..."
	$(GRADLE) bootJar -q
	@echo ">>> Starting auction-service (logs → .logs/app.log)..."
	@mkdir -p .logs
	nohup java -jar $(APP_JAR) > .logs/app.log 2>&1 &
	@echo ">>> Waiting for application to be ready on $(BASE_URL)..."
	@until curl -sf $(BASE_URL)/actuator/health > /dev/null 2>&1 || \
	       curl -sf $(BASE_URL)/auctions        > /dev/null 2>&1; do sleep 2; done
	@echo ">>> auction-service is up."

# ---------------------------------------------------------------------------
# test — run k6 load test e.g. make test AUCTIONS=5 RPS_PER_AUCTION=50 DURATION=2m
# ---------------------------------------------------------------------------
.PHONY: test
test:
	k6 run \
		-e AUCTIONS=$(AUCTIONS) \
		-e RPS_PER_AUCTION=$(RPS_PER_AUCTION) \
		-e DURATION=$(DURATION) \
		-e BASE_URL=$(BASE_URL) \
		k6/load-test.js

# ---------------------------------------------------------------------------
# stop — kill backend process only
# ---------------------------------------------------------------------------
.PHONY: stop
stop:
	@echo ">>> Stopping auction-service..."
	-pkill -f "auction-service.*\.jar" 2>/dev/null || true
	@echo ">>> Done."

# ---------------------------------------------------------------------------
# destroy — kill backend, stop containers and wipe all volumes
# ---------------------------------------------------------------------------
.PHONY: destroy
destroy:
	@echo ">>> Stopping auction-service..."
	-pkill -f "auction-service.*\.jar" 2>/dev/null || true
	@echo ">>> Tearing down infrastructure (volumes included)..."
	docker compose down -v --remove-orphans
	@echo ">>> Done."

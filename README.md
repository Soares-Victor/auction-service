# Auction Service

A real-time item auction backend built as a coding challenge. This is a test environment, not a production deployment. The goal was to design a system capable of handling high concurrency on bid submission while keeping the data consistent and the auction logic correct.

## The Challenge

Build a backend service where sellers can create auctions, buyers can place bids in real-time, and every bid follows FIFO ordering per auction. The system had to handle 2 million concurrent users without row-level contention on the auction table.

## How It Works

The service is split into two clear paths.

The write path is async: when a user places a bid, the API validates the auction is open and immediately publishes a `BidEvent` to Kafka, returning `202 Accepted`. No DB write happens at this point. The Kafka consumer picks up the event, re-validates the business rules, and persists the bid. Real-time updates are pushed to subscribers via WebSocket (STOMP).

The read path is synchronous: auction state (SCHEDULED, OPEN, CLOSED) is computed on the fly from `startTime` and `endTime`. The current highest bid is queried directly from the bids table using a composite index.

## Why These Decisions

**Auction status is computed, not stored.** If status were a column, every auction going from SCHEDULED to OPEN and then to CLOSED would require an UPDATE. With thousands of concurrent readers and writers, that becomes a hot spot. Computing it from timestamps is free, always accurate, and eliminates a whole class of consistency bugs where the DB status drifts from reality.

**Business rules live in the consumer, not the API.** When a bid request arrives at the REST layer, the auction might be open. By the time the Kafka consumer processes that event 50 milliseconds later, the auction might have closed or a higher bid might have already been persisted. Validating only at the API layer would create a race condition. The consumer is the single point where the state is stable and the decision is final.

**Kafka for bid ordering.** Each auction's bids are routed to the same partition using `auctionId` as the partition key. Kafka guarantees ordering within a partition, so bids for any given auction are processed one at a time in the order they arrived. This makes it safe to do "is this bid higher than the current leader" checks without optimistic locking or SELECT FOR UPDATE on the auction row.

**PostgreSQL, not Redis.** The current highest bid for an auction is a simple `SELECT TOP 1 FROM bids WHERE auction_id = ? ORDER BY amount DESC`. With a composite index on `(auction_id, amount DESC)` this is a single index seek. Adding Redis would introduce another infrastructure component, cache invalidation logic, and a consistency surface to maintain, without a meaningful performance gain for this query pattern.

## Tech Stack

- Java 21 with Spring Boot 4
- PostgreSQL 16 via Flyway migrations
- Apache Kafka (Confluent Platform 7.6)
- WebSocket with STOMP (SockJS)
- k6 for load testing

## Requirements

- Docker and Docker Compose
- Java 21
- k6 (`brew install k6` on macOS)
- make

## Running Locally

```bash
# Start infrastructure and the backend (runs in background, logs → .logs/app.log)
make run

# Tail the application logs
tail -f .logs/app.log

# Stop the backend process only (keeps Postgres and Kafka running)
make stop

# Destroy everything: backend, containers and all volumes
make destroy
```

## Running the Load Test

```bash
# Default: 10 auctions, 100 req/sec per auction, 10 minutes
make test

# Custom parameters
make test AUCTIONS=5 RPS_PER_AUCTION=50 DURATION=2m
```

The load test creates the auctions during setup, waits for them to open, then hammers each one at the configured rate. At teardown it prints the final status and highest bid for each auction.

## REST API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/auctions` | List auctions with pagination |
| POST | `/auctions` | Create an auction |
| GET | `/auctions/{id}` | Get a single auction |
| POST | `/auctions/{id}/bids` | Place a bid (returns 202) |
| GET | `/auctions/{id}/bids` | Bid history |
| POST | `/auctions/{id}/comments` | Add a comment |
| GET | `/auctions/{id}/comments` | List comments |

All list endpoints support `?page=0&size=20&sort=field,direction`.

## Real-Time Updates via WebSocket

Connect to the WebSocket endpoint and subscribe to an auction topic to receive live bid and close events.

**Endpoint:** `ws://localhost:8080/ws`

Using a browser or wscat:

```bash
# Install wscat if needed
npm install -g wscat

wscat -c "ws://localhost:8080/ws/websocket"
```

After connecting, send a STOMP CONNECT frame and subscribe to the auction topic:

```
CONNECT
accept-version:1.2
heart-beat:0,0

^@
SUBSCRIBE
id:sub-0
destination:/topic/auctions/{auctionId}

^@
```

You will receive a `BID_PLACED` message every time a bid is accepted:

```json
{
  "type": "BID_PLACED",
  "auctionId": "...",
  "bidId": "...",
  "userId": "alice",
  "amount": 1250.00,
  "timestamp": "2026-03-12T14:00:00Z"
}
```

And an `AUCTION_CLOSED` message when the auction ends:

```json
{
  "type": "AUCTION_CLOSED",
  "auctionId": "...",
  "winnerId": "alice",
  "winningBid": 1250.00,
  "closedAt": "2026-03-12T15:00:00Z"
}
```

A SockJS-compatible client (like the one in the Postman WebSocket tab or a browser using `@stomp/stompjs`) will handle the handshake automatically.

## Potential Improvements

**Dedicated consumer component.** The Kafka consumer currently lives inside the same Spring Boot application. In production you would want to extract it into a separate service so you can scale the consumer fleet independently from the REST API. The number of consumer instances is naturally bounded by the number of Kafka partitions (currently 4), so scaling the API horizontally would not help with bid throughput unless the consumer is separate.

**Datadog metrics.** A few things are worth instrumenting once the service is running under real load: p99 and average latency per endpoint, Kafka consumer lag on the `bid-events` topic (lag spiking means consumers are falling behind and bids are queuing up), and the end-to-end time from when a bid is submitted at the API to when it is persisted and broadcast by the consumer.

**Migrate `application.properties` to `application.yaml`.** The properties file works but YAML is easier to read at a glance, especially for nested keys like the Kafka and JPA configuration blocks.

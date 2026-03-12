import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ---------------------------------------------------------------------------
// Parameters — override via env vars:
//   k6 run -e AUCTIONS=10 -e RPS_PER_AUCTION=100 -e DURATION=10m load-test.js
// ---------------------------------------------------------------------------
const AUCTION_COUNT    = parseInt(__ENV.AUCTIONS          || '10');
const RPS_PER_AUCTION  = parseInt(__ENV.RPS_PER_AUCTION   || '100');
const DURATION         = __ENV.DURATION                   || '10m';
const BASE_URL         = __ENV.BASE_URL                   || 'http://localhost:8080';
const AUCTION_DURATION_MIN = parseInt(__ENV.AUCTION_DURATION_MIN || '25'); // must outlast the test

const TOTAL_RPS = AUCTION_COUNT * RPS_PER_AUCTION;

// ---------------------------------------------------------------------------
// Metrics
// ---------------------------------------------------------------------------
const bidAccepted  = new Counter('bids_accepted');
const bidRejected  = new Counter('bids_rejected');
const bidErrorRate = new Rate('bid_error_rate');
const bidDuration  = new Trend('bid_duration_ms', true);

// ---------------------------------------------------------------------------
// Auction catalogue — 10 realistic items (sliced to AUCTION_COUNT)
// ---------------------------------------------------------------------------
const CATALOGUE = [
  {
    sellerId: 'vintage_motors',
    title: '1967 Ford Mustang Fastback — Restored',
    description: 'Numbers-matching 390 V8, Candy Apple Red, full rotisserie restoration completed in 2023.',
    startingPrice: 45000.00, minBidIncrement: 500.00,
  },
  {
    sellerId: 'luxury_watches_ny',
    title: 'Rolex Daytona Ref. 116500LN — White Dial',
    description: 'Unworn, full sticker, box & papers 2022. Ceramic bezel, oysterflex bracelet.',
    startingPrice: 28000.00, minBidIncrement: 250.00,
  },
  {
    sellerId: 'tech_surplus',
    title: 'Apple Mac Pro (2023) — M2 Ultra 192GB RAM',
    description: '24-core CPU / 76-core GPU, 8TB SSD, AppleCare+ until 2026. Lightly used in studio.',
    startingPrice: 7500.00, minBidIncrement: 100.00,
  },
  {
    sellerId: 'art_collective',
    title: 'Jean-Michel Basquiat — "Untitled" Lithograph (1983)',
    description: 'Hand-signed, numbered 12/50. Certificate of authenticity from Sotheby\'s.',
    startingPrice: 18000.00, minBidIncrement: 200.00,
  },
  {
    sellerId: 'sneaker_vault',
    title: 'Nike Air Yeezy 2 "Red October" — DS Size 10',
    description: 'Deadstock, original box and all accessories. One of the most sought-after sneakers ever made.',
    startingPrice: 5000.00, minBidIncrement: 50.00,
  },
  {
    sellerId: 'estate_sales_co',
    title: 'Patek Philippe Nautilus 5711/1A — Steel',
    description: 'Discontinued model, full set, 2019 purchase date. No visible wear. Private collector sale.',
    startingPrice: 120000.00, minBidIncrement: 1000.00,
  },
  {
    sellerId: 'gaming_liquidators',
    title: 'NVIDIA RTX 5090 FE — Sealed (Lot of 2)',
    description: 'Two factory-sealed Founders Edition cards. Purchased directly from Best Buy.',
    startingPrice: 6000.00, minBidIncrement: 100.00,
  },
  {
    sellerId: 'rare_books_london',
    title: 'First Edition "The Great Gatsby" — F. Scott Fitzgerald (1925)',
    description: 'First printing, first issue. Original dust jacket. Graded CGC 7.5.',
    startingPrice: 35000.00, minBidIncrement: 500.00,
  },
  {
    sellerId: 'camera_estate',
    title: 'Leica M6 TTL 0.85 + Summilux-M 50mm f/1.4 ASPH',
    description: 'Black chrome, light use (~15 rolls). Lens optically perfect, no fungus or haze.',
    startingPrice: 8500.00, minBidIncrement: 100.00,
  },
  {
    sellerId: 'music_memorabilia',
    title: 'Gibson Les Paul 1959 Reissue — Signed by Eric Clapton',
    description: 'Murphy Lab aged finish, COA signed backstage at Royal Albert Hall 2019.',
    startingPrice: 12000.00, minBidIncrement: 150.00,
  },
].slice(0, AUCTION_COUNT);

// Bidder pool — simulates distinct concurrent users
const BIDDERS = Array.from({ length: 200 }, (_, i) => `bidder_${String(i + 1).padStart(3, '0')}`);

// ---------------------------------------------------------------------------
// Setup — create all auctions before the load test begins
// ---------------------------------------------------------------------------
export function setup() {
  const auctionIds = [];

  for (const item of CATALOGUE) {
    const startTime = new Date(Date.now() +  10 * 1000).toISOString();
    const endTime   = new Date(Date.now() + AUCTION_DURATION_MIN * 60 * 1000).toISOString();

    const res = http.post(
      `${BASE_URL}/auctions`,
      JSON.stringify({
        sellerId:        item.sellerId,
        title:           item.title,
        description:     item.description,
        startTime,
        endTime,
        startingPrice:   item.startingPrice,
        minBidIncrement: item.minBidIncrement,
      }),
      { headers: { 'Content-Type': 'application/json' } },
    );

    check(res, { 'auction created (201)': r => r.status === 201 });

    const body = res.json();
    if (body && body.id) {
      auctionIds.push({ id: body.id, item });
      console.log(`Created [${body.id}] ${item.title}`);
    } else {
      console.error(`Failed to create "${item.title}": HTTP ${res.status} — ${res.body}`);
    }
  }

  console.log(`Waiting 15s for ${auctionIds.length} auctions to open...`);
  sleep(15);

  return { auctionIds };
}

// ---------------------------------------------------------------------------
// Load options — driven entirely by parameters
// ---------------------------------------------------------------------------
export const options = {
  scenarios: {
    bid_load: {
      executor:        'constant-arrival-rate',
      rate:            TOTAL_RPS,
      timeUnit:        '1s',
      duration:        DURATION,
      preAllocatedVUs: Math.ceil(TOTAL_RPS * 0.5),
      maxVUs:          TOTAL_RPS * 2,
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.01'],  // <1% HTTP errors
    http_req_duration: ['p(95)<500'],  // p95 under 500ms
    bid_error_rate:    ['rate<0.05'],  // <5% non-202 bid responses
  },
};

// ---------------------------------------------------------------------------
// Default — one bid per iteration, spread evenly across auctions
// ---------------------------------------------------------------------------
export default function (data) {
  const { auctionIds } = data;
  if (!auctionIds || auctionIds.length === 0) return;

  const auction = auctionIds[__ITER % auctionIds.length];
  const userId  = BIDDERS[Math.floor(Math.random() * BIDDERS.length)];
  const { item } = auction;

  // Fetch the current auction state so we always bid above the current leader
  const getRes = http.get(`${BASE_URL}/auctions/${auction.id}`);
  if (getRes.status !== 200) return;

  const auctionState   = getRes.json();
  const currentHighest = auctionState.currentHighestBid != null
    ? parseFloat(auctionState.currentHighestBid)
    : item.startingPrice - item.minBidIncrement; // no bids yet → first valid bid = startingPrice

  // Bid exactly minBidIncrement above current highest + small random jitter (1–5 increments)
  // so competing VUs don't all send the same amount at the same instant
  const jitter = (Math.floor(Math.random() * 5) + 1) * item.minBidIncrement;
  const amount = currentHighest + item.minBidIncrement + jitter;

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/auctions/${auction.id}/bids`,
    JSON.stringify({ userId, amount }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  bidDuration.add(Date.now() - start);

  const ok = check(res, { 'bid accepted (202)': r => r.status === 202 });
  ok ? bidAccepted.add(1) : bidRejected.add(1);
  bidErrorRate.add(ok ? 0 : 1);
}

// ---------------------------------------------------------------------------
// Teardown — print final state of each auction
// ---------------------------------------------------------------------------
export function teardown(data) {
  const { auctionIds } = data;
  console.log('\n=== Final auction state ===');
  for (const auction of auctionIds) {
    const res = http.get(`${BASE_URL}/auctions/${auction.id}`);
    if (res.status === 200) {
      const b = res.json();
      console.log(`  ${b.title} | status=${b.status} | highestBid=${b.currentHighestBid}`);
    }
  }
}

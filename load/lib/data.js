// The load generator's knowledge of the seeded data (see db/init/20-seed.sql).
export const BASE = __ENV.BASE_URL || 'http://localhost:8080';
export const ACCOUNTS = 1000000;
export const ACTIVE_TRADERS = 50000;      // accounts 1..50,000 hold most of the order history

// Where order flow goes: [board, first symbol index, symbol count, share of flow]
const BOARDS = [['HOSE', 0, 400, 0.5], ['HNX', 400, 300, 0.3], ['UPCOM', 700, 900, 0.2]];

export function rnd(n) { return Math.floor(Math.random() * n); }

export function symbolAt(i) {
  return String.fromCharCode(65 + Math.floor(i / 676), 65 + (Math.floor(i / 26) % 26), 65 + (i % 26));
}

export function refPrice(i) { return 10000 + ((i * 37) % 900) * 100; }

export function pickBoard() {
  let r = Math.random();
  for (const b of BOARDS) { if (r < b[3]) return b; r -= b[3]; }
  return BOARDS[0];
}

// A valid order: at the reference price, a round lot, and -- for sells --
// from an account that actually holds the symbol (account g holds #(g % 1600)).
export function randomOrder() {
  const b = pickBoard();
  const i = b[1] + rnd(b[2]);
  const sell = Math.random() < 0.4;
  const accountId = sell ? (i === 0 ? 1600 : i) + 1600 * rnd(624) : 1 + rnd(ACCOUNTS);
  return JSON.stringify({
    accountId, symbol: symbolAt(i), side: sell ? 'SELL' : 'BUY',
    qty: (1 + rnd(10)) * 100, price: refPrice(i),
  });
}

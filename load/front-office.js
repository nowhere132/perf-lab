// What the trading screens do all day: look at the board, look at the account.
import http from 'k6/http';
import { BASE, ACCOUNTS, pickBoard, rnd } from './lib/data.js';
import { plan, params, report } from './lib/harness.js';

export const options = plan('front-office reads');
export const handleSummary = report();

export default function () {
  if (Math.random() < 0.5) {
    const b = pickBoard();
    http.get(`${BASE}/api/market/board/${b[0]}?offset=${rnd(b[2] - 30)}&limit=30`, params);
  } else {
    http.get(`${BASE}/api/accounts/${1 + rnd(ACCOUNTS)}`, params);
  }
}

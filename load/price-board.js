// Trading screens polling the price board, one page of 30 symbols at a time.
import http from 'k6/http';
import { BASE, pickBoard, rnd } from './lib/data.js';
import { plan, params, report } from './lib/harness.js';

export const options = plan('price board');
export const handleSummary = report();

export default function () {
  const b = pickBoard();
  http.get(`${BASE}/api/market/board/${b[0]}?offset=${rnd(b[2] - 30)}&limit=30`, params);
}

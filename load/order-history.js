// Clients opening their order book: recent orders of an active trader.
import http from 'k6/http';
import { BASE, ACTIVE_TRADERS, rnd } from './lib/data.js';
import { plan, named, report } from './lib/harness.js';

export const options = plan('order history');
export const handleSummary = report();

export default function () {
  http.get(`${BASE}/api/accounts/${1 + rnd(ACTIVE_TRADERS)}/orders`, named('GET /api/accounts/{id}/orders'));
}

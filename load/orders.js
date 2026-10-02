// Order placement: 60% buys, 40% sells, flow split HOSE 50 / HNX 30 / UPCOM 20.
import http from 'k6/http';
import { BASE, randomOrder } from './lib/data.js';
import { plan, params, report } from './lib/harness.js';

export const options = plan('order placement');
export const handleSummary = report();

export default function () {
  http.post(`${BASE}/api/orders`, randomOrder(), params);
}

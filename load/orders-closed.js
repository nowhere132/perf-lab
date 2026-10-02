// The load test the team has always run: N virtual users, each sending its
// next order as soon as the previous one returns (a CLOSED model).
//   ./lab k6 orders-closed VUS=20 DURATION=60
import http from 'k6/http';
import { BASE, randomOrder } from './lib/data.js';
import { params } from './lib/harness.js';

http.setResponseCallback(http.expectedStatuses(200, 201));

export const options = {
  discardResponseBodies: true,
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(99)', 'p(99.9)', 'max'],
  vus: Number(__ENV.VUS || 20),
  duration: `${__ENV.DURATION || 60}s`,
};

export default function () {
  http.post(`${BASE}/api/orders`, randomOrder(), params);
}

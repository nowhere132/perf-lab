// Shared shape of every graded run: ramp up (not measured), then a steady
// OPEN-MODEL phase (measured). Arrivals are scheduled by the clock, not by
// the previous response, so a stalled system cannot slow the generator down.
import http from 'k6/http';

http.setResponseCallback(http.expectedStatuses(200, 201));

const num = (v, d) => (v === undefined || v === '' ? d : Number(v));

// Rate, durations and goals come from the environment (./lab sets them from
// the challenge's challenge.env; override any of them on the command line):
//   RATE (req/s)  WARMUP  DURATION (s)  GOAL_P50  GOAL_P99  GOAL_P999 (ms)  GOAL_ERRORS (ratio)
const meta = {};

export function plan(title) {
  const rate = num(__ENV.RATE, 50);
  const warm = num(__ENV.WARMUP, 30);
  const dur = num(__ENV.DURATION, 60);
  const g = { p50: num(__ENV.GOAL_P50), p99: num(__ENV.GOAL_P99), p999: num(__ENV.GOAL_P999), errors: num(__ENV.GOAL_ERRORS) };
  Object.assign(meta, { rate, dur, title: __ENV.TITLE || title });
  const vus = Math.min(4000, Math.max(50, rate * 2));

  const lat = [];
  if (g.p50) lat.push(`p(50)<${g.p50}`);
  if (g.p99) lat.push(`p(99)<${g.p99}`);
  if (g.p999) lat.push(`p(99.9)<${g.p999}`);

  return {
    discardResponseBodies: true,
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(99)', 'p(99.9)', 'max'],
    scenarios: {
      warmup: {
        executor: 'ramping-arrival-rate', startRate: Math.ceil(rate / 10), timeUnit: '1s',
        stages: [{ target: rate, duration: `${Math.ceil(warm * 0.6)}s` }, { target: rate, duration: `${Math.floor(warm * 0.4)}s` }],
        preAllocatedVUs: Math.ceil(vus / 4), maxVUs: vus, tags: { phase: 'warmup' }, gracefulStop: '0s',
      },
      measure: {
        executor: 'constant-arrival-rate', rate, timeUnit: '1s', startTime: `${warm}s`, duration: `${dur}s`,
        preAllocatedVUs: Math.ceil(vus / 4), maxVUs: vus, tags: { phase: 'measure' }, gracefulStop: '5s',
      },
    },
    thresholds: {
      'http_req_duration{phase:measure}': lat.length ? lat : ['max>=0'],
      'http_req_failed{phase:measure}': [`rate<${g.errors === undefined ? 0.001 : g.errors}`],
      'http_reqs{phase:measure}': [`count>=${Math.floor(rate * dur * 0.98)}`],
    },
  };
}

export const params = { timeout: '10s', headers: { 'Content-Type': 'application/json' } };

// Requests whose URL carries an id or offset must be grouped under one `name`
// tag, otherwise k6 creates a time series per distinct URL (100k+ in a graded run).
export const named = (name) => ({ ...params, tags: { name } });

const ms = (v) => (v === undefined ? '-' : v >= 1000 ? `${(v / 1000).toFixed(2)}s` : `${v.toFixed(1)}ms`);

export function report() {
  return (data) => {
    const m = data.metrics;
    const d = (m['http_req_duration{phase:measure}'] || {}).values || {};
    const f = (m['http_req_failed{phase:measure}'] || {}).values || {};
    const n = ((m['http_reqs{phase:measure}'] || {}).values || {}).count || 0;
    const dropped = ((m.dropped_iterations || {}).values || {}).count || 0;
    const { rate, dur, title } = meta;

    const goals = [];
    const names = { 'http_req_duration{phase:measure}': 'latency', 'http_req_failed{phase:measure}': 'errors', 'http_reqs{phase:measure}': 'completed' };
    for (const k of Object.keys(names)) {
      for (const [expr, t] of Object.entries((m[k] || {}).thresholds || {})) {
        goals.push({ metric: names[k], goal: expr.replace(/\s/g, ''), ok: t.ok });
      }
    }
    const pass = goals.length > 0 && goals.every((x) => x.ok);

    const out = [
      '',
      `  ${title}`,
      `  offered   ${rate}/s for ${dur}s (open model)      completed ${n} = ${(n / dur).toFixed(0)}/s      dropped ${dropped}`,
      `  latency   p50 ${ms(d.med)}   p90 ${ms(d['p(90)'])}   p99 ${ms(d['p(99)'])}   p99.9 ${ms(d['p(99.9)'])}   max ${ms(d.max)}`,
      `  errors    ${((f.rate || 0) * 100).toFixed(2)}%`,
      '',
      ...goals.map((x) => `  ${x.ok ? 'ok  ' : 'MISS'}  ${x.metric.padEnd(10)} ${x.goal}`),
      '',
    ].join('\n');

    const result = {
      title, pass, rate, duration: dur, completed: n, dropped, errorRate: f.rate || 0,
      p50: d.med, p90: d['p(90)'], p99: d['p(99)'], p999: d['p(99.9)'], max: d.max, goals,
    };
    const files = { stdout: out };
    if (__ENV.SUMMARY) files[__ENV.SUMMARY] = JSON.stringify(result, null, 2);
    return files;
  };
}

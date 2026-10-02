## Hint 1
Run `./lab test 2 RATE=200`, then `RATE=300`. One passes comfortably and the
other collapses. A system that goes from fine to seconds within a 50 % load
increase, with idle CPUs, is queueing for something that comes in a fixed
quantity. Find what there are exactly N of.

## Hint 2
While the failing test runs, look at the Saturation row in Grafana, then run
`./lab threads`. Every request thread is parked at the same place. Now
`./lab ash`: the sessions are not running SQL, they are `idle in transaction`.
The database is waiting for the application.

## Hint 3
What is the application doing while it holds a transaction open? Read
`OrderService.place` with one question in mind: between the first statement
and the commit, which lines do not need the database at all? Then do the
arithmetic: 10 connections, each held for one whole order (~35 ms), is a
ceiling of about 280 orders/s.

## Hint 4
Making the pool bigger is the wrong direction (and against the rules). Shorten
the time a connection is held instead. The risk call needs no database state,
so it can happen before any transaction starts. The venue call must come after
the reservation is committed, so it splits the work in two: one short
transaction that reserves and records the order as NEW, the venue call with no
transaction open, then a second short transaction that records the outcome.
`TransactionTemplate` makes that shape easy to write. Remember the failure
path: if the venue call throws, release the reservation.

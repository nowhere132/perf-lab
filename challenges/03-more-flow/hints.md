## Hint 1
CPU idle, database idle, pool idle, and still a hard ceiling near 500
orders/s. Threads are waiting, but not for anything that has a gauge on the
dashboard. `./lab threads` during the failing run shows the state of the
request threads and the application frame each one is stuck in.

## Hint 2
Most request threads are `BLOCKED` in `VenueSession.send`. It is
`synchronized`, and the network round trip to the exchange (~3 ms) happens
inside the lock. One session per venue means one order at a time per venue:
about 1 / 3.6 ms = 280 orders/s for HOSE, which takes half the flow, so about
500 to 550 in total.

## Hint 3
The lock is not a mistake. Read the venue rules in
`exchange-sim/ExchangeSim.java`: messages on a session must arrive in sequence,
and the only way to guarantee that over request/response is to send one at a
time. Shrinking the lock to cover just the sequence counter lets two sends
overtake each other on the wire, and the venue rejects the gap. The same rules
contain the way out.

## Hint 4
A member may hold up to 16 sessions per venue. When a lock protects a real
invariant of one resource, get more of the resource. Give each venue a pool of
sessions and let each order borrow a free one (a `BlockingQueue<VenueSession>`
does it in a few lines). Size it from the flow: 400 orders/s to HOSE at 3.6 ms
each keeps about 1.5 sessions busy on average, so a handful is plenty.

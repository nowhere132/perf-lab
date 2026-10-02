## Hint 1
Run both tests and put the numbers side by side. The closed-model test reports
a good p99. The open-model test, against the same system a minute later,
reports a p99 several times worse. Neither is lying about what it measured.
Work out what each one does during a half-second in which the server answers
nobody.

## Hint 2
With 20 virtual users, a half-second stall delays exactly 20 requests, then
the users sit and wait, sending nothing. In the real world 250 orders arrive
during that half second and every one of them is late. The closed model
"coordinates" with the server: it stops asking when the server stops
answering, and so leaves the slow samples out. That is coordinated omission,
and it is why this lab's graded tests use an arrival rate.

## Hint 3
Now find the stall. It is periodic, which narrows things a lot. Open the
latency panel in Grafana with a short refresh, or print timestamps of slow
responses: the spikes come every ten seconds. The brief names one thing that
happens every ten seconds. Catch it in the act:

    while true; do ./lab threads | head -6; sleep 1; done

## Hint 4
During the stall request threads are waiting in `ReferenceData.find` for a
read lock, because `reload()` holds the write lock for its entire run, and
that run makes one query per symbol (1,600 of them, about half a second).
Two things to fix, and either one alone would pass, but do both: load the data
without holding any lock (build a new map on the side, then publish it by
assigning one `volatile` reference, so readers never wait), and load it in two
queries rather than 1,601.

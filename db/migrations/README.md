Your schema changes go here as `NNN-description.sql` (indexes, constraints, ...).

    ./lab migrate        # applies every file not yet applied, in name order

Each file runs once as the database owner and is recorded in `lab_migrations`.
`./lab reseed` rebuilds the database from `db/init/` and then re-applies these.

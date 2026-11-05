# Datahike native size experiment

Targets: roughly 10 MB compressed, startup through query under 35 ms if possible
(or 100–150 ms for a substantially smaller binary), and no database-size growth
in startup time. Community GraalVM only; no PGO or database compression.

Datahike is pinned to 0.8.1913. Builds use Community GraalVM 25, `-O2`, direct
linking, and `:elide-meta [:doc :file :line :added]`. Build-time initialization
is unchanged from upstream. Set `JAVA_HOME` to Community GraalVM, then run:

```sh
bash build.sh api
# or:
bash build.sh local
```

`api` uses the public facade. `local` calls the same connect/query/pull/transact
functions through their implementation namespaces, without loading the facade's
HTTP backend and API schema registration. Those namespaces are marked `no-doc`
upstream: this is a version-pinned experiment, not a stable public API promise.
Neither profile modifies library source or private Vars.

GitHub Actions builds both profiles, prepares persistent databases in separate
native CLI processes, and measures 2, 10,000, 100,000 and 1,000,000 notes. Each
measurement includes a fresh process, open, parameterized lookup, release and
exit. Twenty runs after an initial run; OS caches are warm. Separate smoke tests
cover parameters, pull, registered functions, rules, recursive rules, writes,
and reading a committed write in a new process.

Baseline before metadata elision: 122.12 MiB executable, 35.52 MiB gzip (`-O2`).
The earlier quick build was 123.30 MiB executable, 36.96 MiB gzip. Stripping alone
saved 6.6 KiB; most of its apparent savings came from changing the ad-hoc signature.

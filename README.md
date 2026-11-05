# Datahike native experiment

Datahike 0.8.1913; Community GraalVM 25.5.5.0.3, Apple Silicon.

`bash build.sh` compiles a fixed, parameterized lookup against Datahike core.
It follows the upstream native build: AOT with direct linking and build-time
initialization. No private API, lifecycle adapter, S3 backend, or pod support.
The shared toolchain and Maven cache live in the parent experiment directory.
`-O2` enables compiler optimizations; no PGO.

```sh
python3 scripts/create-test-db.py
bash build.sh
./target/datahike-demo target/config-2.edn 2
python3 benchmark.py
```

Fixture preparation uses the upstream Community-built native CLI at
`target/upstream/dthk`, with normal transactions and default file-store settings.
Each transaction runs in a separate process; writers have exited before reads.
Existing fixtures are never overwritten. The upstream CLI's optional
`:in-place? true` configuration failed on a fresh store, so it is omitted.

Benchmarks include process launch, database open, parameterized lookup, release,
and exit. They alternate first/last IDs across 20 fresh processes after one
initial run. OS disk caches remain warm. No database compression is applied.

## Prebuilt native CLI results

Compiler stopped before measurement. Two passes; the table uses the second
pass, in reverse database-size order. Twenty fresh processes per database,
warm OS caches, alternating lookup IDs 1 and N. Short titles (`Note N`).

| Notes | Store on disk | Median | p95 |
| ---: | ---: | ---: | ---: |
| 2 | 88 KiB | 28.5 ms | 28.8 ms |
| 10,000 | 3.1 MiB | 58.9 ms | 59.5 ms |
| 100,000 | 32 MiB | 32.7 ms | 33.9 ms |
| 1,000,000 | 333 MiB | 32.2 ms | 34.0 ms |

The 10,000-note slowdown reproduced in both passes; its cause is uninvestigated.
The first launch of the first pass took 410 ms; these are not cold-disk claims.
Raw measurements: `target/benchmark-dthk.json` and
`target/benchmark-dthk-first-pass.json`. The core-only build was stopped at the
user's request; no size-reduction result is available.

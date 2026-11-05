"""Fresh native processes: database open, parameterized lookup, release, exit."""
import json
import os
from pathlib import Path
import statistics
import subprocess
import sys
import time

root = Path(__file__).resolve().parent
binary = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "target/datahike-demo"
counts = list(map(int, sys.argv[2:])) or [2, 10000, 100000, 1000000]
env = dict(os.environ, PATH="/nonexistent")
for key in ("JAVA_HOME", "JAVA_CMD", "CLASSPATH", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS"):
    env.pop(key, None)
report = {}
for count in counts:
    config = str(root / f"target/config-{count}.edn")
    times = []
    for i in range(21):
        identifier = 1 if i % 2 else count
        args = [str(binary), config, str(identifier)]
        if binary.name == "dthk":
            args = [str(binary), "-f", "json", "query",
                    '[:find ?title . :in $ ?id :where [?e :note/id ?id] [?e :note/title ?title]]',
                    "db:" + config, str(identifier)]
        start = time.perf_counter_ns()
        result = subprocess.run(args, env=env, check=True, capture_output=True, text=True)
        elapsed = (time.perf_counter_ns() - start) / 1_000_000
        assert json.loads(result.stdout) == f"Note {identifier}", result.stdout
        times.append(elapsed)
    report[str(count)] = {"first_ms": times[0], "median_ms": statistics.median(times[1:]),
                          "p95_ms": sorted(times[1:])[18], "runs_ms": times[1:]}
output = root / f"target/benchmark-{binary.name}.json"
output.write_text(json.dumps(report, indent=2) + "\n")
print(json.dumps(report, indent=2))

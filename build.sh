#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-$PWD/../.tools/graalvm}"
export JAVA_CMD="$JAVA_HOME/bin/java"
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p target/classes
clojure -Srepro -M -e '(binding [*compile-path* "target/classes" *compiler-options* {:direct-linking true}] (compile (quote experiment.main)))'
experiment_cp="$(clojure -Srepro -Spath)"
native-image -J-Xmx5g -J-XX:-UseParallelGC -J-XX:+UseG1GC \
  -J-Xlog:gc:file=target/build-gc.log --parallelism=2 -Ob \
  --initialize-at-build-time -H:+AllowDeprecatedInitializeAllClassesAtBuildTime \
  --no-fallback -H:IncludeResources=DATAHIKE_VERSION -H:IncludeResources=DATAHIKE_GIT_SHA \
  -cp "$experiment_cp" experiment.main target/datahike-demo

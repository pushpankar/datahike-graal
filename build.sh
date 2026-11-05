#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-$PWD/../.tools/graalvm}"
export JAVA_CMD="$JAVA_HOME/bin/java"
export PATH="$JAVA_HOME/bin:$PATH"
profile="${1:-api}"
extra_flag=
case "$profile" in
  api) entry=experiment.main ;;
  local) entry=experiment.local ;;
  relational) entry=experiment.local; export DATAHIKE_QUERY_PLANNER=false ;;
  safe) entry=experiment.safe ;;
  precise) entry=experiment.safe; extra_flag=-H:-RemoveSaturatedTypeFlows ;;
  *) echo 'Usage: build.sh [api|local|relational|safe|precise]' >&2; exit 1 ;;
esac
optimization="${DATAHIKE_OPTIMIZATION:-2}"
case "$optimization" in 2|s) ;; *) echo 'Optimization must be 2 or s' >&2; exit 1 ;; esac
rm -rf target/classes
mkdir -p target/classes
clojure -Srepro -M -e "(binding [*compile-path* \"target/classes\" *compiler-options* {:direct-linking true :elide-meta [:doc :file :line :added]}] (compile '$entry))"
experiment_cp="$(clojure -Srepro -Spath)"
native-image -J-Xmx5g -J-XX:-UseParallelGC -J-XX:+UseG1GC \
  -J-Xlog:gc:file=target/build-gc.log "-O$optimization" \
  --initialize-at-build-time -H:+AllowDeprecatedInitializeAllClassesAtBuildTime \
  --no-fallback -H:IncludeResources=DATAHIKE_VERSION -H:IncludeResources=DATAHIKE_GIT_SHA \
  ${extra_flag:+"$extra_flag"} \
  -cp "$experiment_cp" "$entry" target/datahike-demo

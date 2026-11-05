#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-$PWD/.tools/graalvm}"
export JAVA_CMD="$JAVA_HOME/bin/java"
export PATH="$JAVA_HOME/bin:$PATH"
rm -rf target/classes target/native-config
mkdir -p target/classes
clojure -Srepro -M -e '(binding [*compile-path* "target/classes" *compiler-options* {:direct-linking true}] (compile (quote experiment.main)))'
experiment_cp="$(clojure -Srepro -Spath)"
java -agentlib:native-image-agent=config-output-dir=target/native-config -cp "$experiment_cp" clojure.main scripts/create-test-db.clj target/test-db
java -agentlib:native-image-agent=config-merge-dir=target/native-config -cp "$experiment_cp" experiment.main query target/test-db 1
java -agentlib:native-image-agent=config-merge-dir=target/native-config -cp "$experiment_cp" experiment.main test target/test-db 1
javac --add-modules org.graalvm.nativeimage -cp "$experiment_cp" -d target/classes build/DatomicImage.java
native-image -H:+PrintUniverse -J-Xmx4g -O2 -march=native --features=DatomicImage \
  --initialize-at-build-time=clojure,datomic,cognitect,experiment,com.github.benmanes.caffeine,org.fressian \
  --initialize-at-run-time=com.github.benmanes.caffeine.cache.UnsafeRefArrayAccess \
  '-H:ExcludeResources=.*\.clj[c]?$' \
  -H:ConfigurationFileDirectories=target/native-config \
  -cp "$experiment_cp" experiment.main target/datomic-demo

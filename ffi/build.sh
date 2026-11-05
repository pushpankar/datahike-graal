#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${GRAALVM_HOME:-$PWD/../.tools/graalvm}"
export JAVA_CMD="$JAVA_HOME/bin/java"
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p target/classes
clojure -Srepro -M -e '(binding [*compile-path* "target/classes" *compiler-options* {:direct-linking true}] (compile (quote experiment.ffi)))'
sample_cp="$(clojure -Srepro -Spath)"
javac --add-modules org.graalvm.nativeimage -cp "$sample_cp" -d target/classes build/*.java
native-image -J-Xmx2g --shared -O2 --features=SampleFeature \
  --initialize-at-build-time=clojure,experiment,SampleLibrary \
  '-H:ExcludeResources=.*\.clj[c]?$' \
  -cp "$sample_cp" -o target/libsample

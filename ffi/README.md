# Chez → Graal → Clojure sample

`sample_add` exports the compiled Clojure `experiment.ffi/add` function through
Graal's C ABI. `test.ss` loads the library, creates one isolate, calls the function
using Chez `foreign-procedure`, then tears down the isolate. No JVM is launched.

```sh
bash ffi/build.sh
cd ffi
chez --script test.ss
```

Uses Community GraalVM from the parent experiment. This sample tests signed 64-bit
arguments and return values, including negatives and values above 32 bits.
Datomic is not included in this first boundary test.

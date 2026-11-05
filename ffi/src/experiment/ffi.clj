(ns experiment.ffi)

(defn add [^long a ^long b]
  (unchecked-add a b))

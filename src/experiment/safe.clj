(ns experiment.safe
  (:require [experiment.local :as local]
            [datahike.query.resolve :as resolve])
  (:gen-class))

;; Public application configuration: native queries use built-ins and explicitly
;; registered functions, without resolving arbitrary JVM Vars or methods.
(alter-var-root #'resolve/*symbol-resolver* (constantly resolve/safe-symbol-resolver))

(defn -main [config-file id]
  (local/-main config-file id))

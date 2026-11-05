(ns experiment.relational
  (:require [experiment.local :as local]
            [datahike.query :as query])
  (:gen-class))

(assert query/*disable-planner* "DATAHIKE_QUERY_PLANNER must be false during image building")

(defn -main [config-file id]
  (local/-main config-file id))

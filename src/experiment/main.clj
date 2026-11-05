(ns experiment.main
  (:require [clojure.edn :as edn]
            [datahike.api :as d]
            [datahike.query.execute]
            [experiment.checks :as checks])
  (:gen-class))

(defn -main [config-file id]
  (let [conn (d/connect (edn/read-string (slurp config-file)))]
    (try
      (if (= id "check")
        (checks/run! conn d/q d/pull d/transact)
        (prn (d/q '[:find ?title . :in $ ?id
                    :where [?e :note/id ?id] [?e :note/title ?title]]
                  @conn (Long/parseLong id))))
      (finally (d/release conn))))
  (shutdown-agents))

(ns experiment.local
  ;; Same functions exposed by datahike.api, without loading its whole facade.
  ;; These namespaces are marked no-doc upstream; this experiment pins Datahike.
  (:require [clojure.edn :as edn]
            [datahike.connector :as connector]
            [datahike.query :as query]
            [datahike.query.execute]
            [datahike.pull-api :as pull]
            [datahike.api.impl :as impl]
            [experiment.checks :as checks])
  (:gen-class))

(defn -main [config-file id]
  (let [conn (connector/connect (edn/read-string (slurp config-file)))]
    (try
      (if (= id "check")
        (checks/run! conn query/q pull/pull impl/transact)
        (prn (query/q '[:find ?title . :in $ ?id
                        :where [?e :note/id ?id] [?e :note/title ?title]]
                      @conn (Long/parseLong id))))
      (finally (connector/release conn))))
  (shutdown-agents))

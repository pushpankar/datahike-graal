(ns experiment.main
  (:require [datomic.client.api :as d]
            [experiment.checks :as checks]
            [experiment.image :as image]
            ;; Include Local's dependencies in AOT compilation.
            [datomic.local])
  (:gen-class))

(set! *warn-on-reflection* true)

(def query-by-id
  '[:find ?title :in $ ?id :where [?note :note/id ?id] [?note :note/title ?title]])

(defn -main [command directory & [id]]
  (try
    (image/start!)
    (let [client (d/client {:server-type :datomic-local
                            :system "experiment"
                            :storage-dir (.getAbsolutePath (java.io.File. ^String directory))})]
      (when (= command "seed")
        (d/create-database client {:db-name "notes"}))
      (let [connection (d/connect client {:db-name "notes"})]
        (case command
          "seed"
          (do
            (d/transact connection
              {:tx-data [{:db/ident :note/id :db/valueType :db.type/long
                          :db/cardinality :db.cardinality/one :db/unique :db.unique/identity}
                         {:db/ident :note/title :db/valueType :db.type/string
                          :db/cardinality :db.cardinality/one}]})
            (d/transact connection {:tx-data [{:note/id 1 :note/title "First note"}
                                               {:note/id 2 :note/title "Second note"}]})
            (println "Seeded two notes"))
          "query"
          (prn (d/q {:query query-by-id :args [(d/db connection) (Long/parseLong id)]}))
          "test"
          (checks/run! connection (Long/parseLong id))
          (throw (ex-info "Usage: seed <directory> | query/test <directory> <id>" {})))))
    (shutdown-agents)
    (System/exit 0)
    (catch Throwable error
      (.printStackTrace error)
      (shutdown-agents)
      (System/exit 1))))

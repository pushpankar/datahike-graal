(require '[datomic.client.api :as d]
         '[datomic.local :as local])

(let [directory (or (first *command-line-args*) "target/test-db")
      config {:server-type :datomic-local :system "experiment"
              :storage-dir (.getAbsolutePath (java.io.File. directory))}
      client (d/client config)]
  (d/create-database client {:db-name "notes"})
  (let [connection (d/connect client {:db-name "notes"})]
    (try
      (d/transact connection
        {:tx-data [{:db/ident :note/id :db/valueType :db.type/long
                    :db/cardinality :db.cardinality/one :db/unique :db.unique/identity}
                   {:db/ident :note/title :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one}
                   {:db/ident :test/id :db/valueType :db.type/long
                    :db/cardinality :db.cardinality/one :db/unique :db.unique/identity}
                   {:db/ident :test/name :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one}
                   {:db/ident :test/children :db/valueType :db.type/ref
                    :db/cardinality :db.cardinality/many}]})
      (d/transact connection
        {:tx-data [{:db/id "root" :test/id 1 :test/name "root" :test/children ["branch" "leaf"]}
                   {:db/id "branch" :test/id 2 :test/name "branch" :test/children ["deep"]}
                   {:db/id "leaf" :test/id 3 :test/name "leaf"}
                   {:db/id "deep" :test/id 4 :test/name "deep" :test/children ["tail"]}
                   {:db/id "tail" :test/id 5 :test/name "tail" :test/children ["branch"]}
                   {:test/id 9 :test/name "unrelated"}]})
      (doseq [ids (partition-all 1000 (range 1 10001))]
        (d/transact connection
          {:tx-data (mapv (fn [id]
                           {:note/id id
                            :note/title (case id
                                          1 "First note"
                                          2 "Second note"
                                          (str "Generated note " id))}) ids)}))
      (let [count (ffirst (d/q {:query '[:find (count ?e) :where [?e :note/id]]
                                :args [(d/db connection)]}))]
        (when (not= count 10000)
          (throw (ex-info "Unexpected fixture size" {:notes count})))
        (println "Verified" count "notes in" directory))
      (finally
        (local/release-db (assoc config :db-name "notes"))
        (shutdown-agents))))
  (println "Database closed; ready for query tests."))

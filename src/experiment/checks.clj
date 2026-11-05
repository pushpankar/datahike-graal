(ns experiment.checks
  (:require [datahike.query.resolve :as resolve]))

(defn plus-one [x] (inc x))
(resolve/register-fn! 'experiment.checks/plus-one plus-one)

(defn run! [conn q pull transact]
  (assert (= "Note 2" (q '[:find ?title . :in $ ?id
                            :where [?e :note/id ?id] [?e :note/title ?title]] @conn 2)))
  (assert (= {:note/id 2 :note/title "Note 2"}
             (pull @conn [:note/id :note/title] [:note/id 2])))
  (assert (= 3 (q '[:find ?next . :in $ ?id
                    :where [?e :note/id ?id]
                           [(experiment.checks/plus-one ?id) ?next]] @conn 2)))
  (transact conn [{:db/ident :note/next :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}])
  (transact conn [{:db/id [:note/id 1] :note/next [:note/id 2]}
                  {:db/id [:note/id 2] :note/next [:note/id 3]}])
  (let [query '[:find ?id :in $ % ?start
                :where [?e :note/id ?start] (reachable ?e ?target)
                       [?target :note/id ?id]]]
    (assert (= #{[2]} (q query @conn
                        '[[(reachable ?a ?b) [?a :note/next ?b]]] 1)))
    (assert (= #{[2] [3]} (q query @conn
                             '[[(reachable ?a ?b) [?a :note/next ?b]]
                               [(reachable ?a ?b) [?a :note/next ?middle]
                                                  (reachable ?middle ?b)]] 1))))
  (transact conn [{:note/id 2 :note/title "Updated note"}])
  (assert (= "Updated note" (q '[:find ?title . :in $ ?id
                                  :where [?e :note/id ?id] [?e :note/title ?title]] @conn 2)))
  (println "Parameters, pull, functions, rules, recursion and writes passed."))

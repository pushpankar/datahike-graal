(ns experiment.checks
  (:refer-clojure :exclude [run!])
  (:require [clojure.test :refer [deftest is run-tests]]
            [datomic.client.api :as d]))

(def ^:dynamic *db*)
(def ^:dynamic *id*)
(def names {1 "root" 2 "branch" 3 "leaf" 4 "deep" 5 "tail" 9 "unrelated"})

(defn query [form & args]
  (d/q {:query form :args (into [*db*] args)}))

(def by-id
  '[:find ?name :in $ ?id :where [?e :test/id ?id] [?e :test/name ?name]])

(deftest query-parameters
  (is (= [[(names *id*)]] (query by-id *id*)))
  (is (= [["root"]] (query by-id 1)))
  (is (= [["branch"]] (query by-id 2)))
  (is (empty? (query by-id 999)))
  (is (= [["Generated note 10000"]]
         (query '[:find ?title :in $ ?id
                  :where [?e :note/id ?id] [?e :note/title ?title]] 10000)))
  (is (= #{[1 "root"] [9 "unrelated"]}
         (set (query '[:find ?id ?name :in $ [?id ...]
                       :where [?e :test/id ?id] [?e :test/name ?name]] [1 9])))))

(deftest pull
  (is (= {:test/id *id* :test/name (names *id*)}
         (d/pull *db* {:eid [:test/id *id*] :selector [:test/id :test/name]})))
  (is (= [[{:test/id *id* :test/name (names *id*)}]]
         (query '[:find (pull ?e pattern) :in $ pattern ?id
                  :where [?e :test/id ?id]] [:test/id :test/name] *id*)))
  (is (= {:test/name "root" :test/children #{2 3}}
         (update (d/pull *db* {:eid [:test/id 1]
                              :selector [:test/name {:test/children [:test/id]}]})
                 :test/children #(set (map :test/id %))))))

(def related-query
  '[:find ?id :in $ % ?root-id
    :where [?root :test/id ?root-id] (related ?root ?child) [?child :test/id ?id]])

(def direct-rules
  '[[(related [?parent] ?child) [?parent :test/children ?child]]])

(def recursive-rules
  '[[(related [?parent] ?child) [?parent :test/children ?child]]
    [(related [?parent] ?child)
     [?parent :test/children ?middle] (related ?middle ?child)]])

(deftest rules
  (is (= ({1 #{[2] [3]} 2 #{[4]}} *id*)
         (set (query related-query direct-rules *id*))))
  (is (empty? (query related-query direct-rules 9))))

(deftest recursive-rules-with-cycle
  (is (= ({1 #{[2] [3] [4] [5]} 2 #{[2] [4] [5]}} *id*)
         (set (query related-query recursive-rules *id*))))
  (is (empty? (query related-query recursive-rules 9))))

(defn name-matches? [actual expected] (= actual expected))
(defn prefix-name [prefix name] (str prefix name))

(deftest custom-query-functions
  (is (= #{[*id*]}
         (set (query '[:find ?id :in $ ?expected
                       :where [?e :test/id ?id] [?e :test/name ?name]
                       [(experiment.checks/name-matches? ?name ?expected)]]
                     (names *id*)))))
  (let [prefix (str "runtime-" *id* ":")]
    (is (= [[(str prefix (names *id*))]]
           (query '[:find ?label :in $ ?id ?prefix
                    :where [?e :test/id ?id] [?e :test/name ?name]
                    [(experiment.checks/prefix-name ?prefix ?name) ?label]]
                  *id* prefix)))))

(defn run! [connection id]
  (binding [*db* (d/db connection) *id* id]
    (let [{:keys [fail error] :as result} (run-tests 'experiment.checks)]
      (when (pos? (+ fail error))
        (throw (ex-info "Runtime query tests failed" result))))))

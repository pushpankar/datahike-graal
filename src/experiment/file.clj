(ns experiment.file
  (:require [experiment.local :as local]
            [konserve.store :as store])
  (:gen-class))

;; Keep the file backend; remove the optional backends from the dispatch tables.
(doseq [dispatch [store/-connect-store store/-create-store store/-store-exists?
                  store/-delete-store store/-release-store]
        backend [:memory :tiered]]
  (remove-method dispatch backend))

(defn -main [config-file id]
  (local/-main config-file id))

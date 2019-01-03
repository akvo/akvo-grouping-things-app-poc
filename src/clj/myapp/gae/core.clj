(ns myapp.gae.core
  (:require
    [myapp.gae.entity :as entity]
    [myapp.db.user :as db-user]
    [akvo.commons.gae :as gae]
    [akvo.commons.gae.query :as query]
    [clojure.java.io :refer (writer)]
    [clojure.set :as set]))

(defn iter [next-thunk next-fn]
  (lazy-seq
    (let [result (next-thunk)]
      (when (seq result)
        (concat result
          (iter (partial next-fn result) next-fn))))))

(defn store-reducible [kind]
  (reify clojure.lang.IReduceInit
    (reduce [this f init]
      (gae/with-datastore [ds {:hostname "localhost"
                               :port 8888}]
        (let [query (.prepare ds (query/query {:kind kind
                                               :sort-by "createdDateTime"}))
              batch-size 300]
          (loop [state init
                 items (iter
                         #(.asQueryResultList query (query/fetch-options {:limit batch-size}))
                         (fn [query-result]
                           (let [cursor (.getCursor query-result)]
                             (.asQueryResultList query
                               (query/fetch-options {:limit batch-size
                                                     :start-cursor cursor})))))]
            (if (reduced? state)
              @state
              (if-let [item (first items)]
                (recur (f state item) (rest items))
                state))))))))

(comment

  ;; run this
  (do
    (dotimes [_ 3]
      (into []
        (comp
          (map entity/ds-to-clj)
          (map #(select-keys % [:name :projectType :parentId :myapp.gae.entity/key]))
          (map #(set/rename-keys % {:myapp.gae.entity/key :flow-id}))
          (map (fn [v]
                 (try
                   (db-user/upsert-node v)
                   ;; TODO: run this a couple of times until there are no errors
                   ;; In prod: save updates with unknown parents for later and retry after the parent has been
                   ;; updated. Alert if parent is "never" found.
                   (catch AssertionError e (println "No parent found"))))))
        (store-reducible "SurveyGroup")))

    (into []
      (comp
        (map entity/ds-to-clj)
        (map #(select-keys % [:superAdmin :emailAddress :userName :myapp.gae.entity/key]))
        (map #(set/rename-keys % {:myapp.gae.entity/key :flow-id
                                  :emailAddress :email}))
        (map db-user/upsert-user!))
      (store-reducible "User"))

    (into []
      (comp
        (map entity/ds-to-clj)
        (map #(select-keys % [:permissions :name :myapp.gae.entity/key]))
        (map #(set/rename-keys % {:myapp.gae.entity/key :flow-id}))
        (map db-user/upsert-role))
      (store-reducible "UserRole"))

    (into []
      (comp
        (map entity/ds-to-clj)
        (map #(select-keys % [:userId :roleId :securedObjectId :myapp.gae.entity/key]))
        (map #(set/rename-keys % {:myapp.gae.entity/key :flow-id}))
        (map db-user/upsert-user-auth))
      (store-reducible "UserAuthorization")))



  (transduce
    (comp
      (map myapp.gae-entity/ds-to-clj)
      (map println)
      (take 2))
    (constantly nil)
    {}
    (store-reducible))

  )
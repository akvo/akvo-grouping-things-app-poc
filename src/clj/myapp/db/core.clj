(ns myapp.db.core
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as log]
    [conman.core :as conman]
    [java-time :as jt]
    [myapp.config :refer [env]]
    [mount.core :refer [defstate]]
    [myapp.rules :as rules])
  (:import org.postgresql.util.PGobject
           java.sql.Array
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector
           [java.sql
            BatchUpdateException
            PreparedStatement]))

(defstate ^:dynamic *db*
  :start (if-let [jdbc-url (env :database-url)]
           (conman/connect! {:jdbc-url jdbc-url})
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn next-id []
  (:nextval (first (jdbc/query *db* "select NEXTVAL('folder_id')"))))

(defn with-ids
  ([t]
   (with-ids nil t))
  ([parent-path t]
   (let [id (next-id)
         full-path (if parent-path
                     (str parent-path "." id)
                     (str id))
         new-t (assoc t :id id
                        :full-path full-path)]
     (cond-> new-t
       (:children new-t) (update :children (fn [c] (mapv (partial with-ids full-path) c)))))))

(defrecord ltree [v])

(defn insert! [t]
  (jdbc/insert! *db* "folders"
    (-> t
      (select-keys [:id :name :full-path :type :flow-id])
      (clojure.set/rename-keys {:full-path :parent_path :flow-id :flow_id})
      (update :parent_path ->ltree)))
  (doseq [c (:children t)]
    (insert! c)))



(comment

  (do
    (doseq [f (->>
                (java.io.File. "/Users/dlebrero/projects/akvo/gae-to-kafka-initial-import/")
                .listFiles
                (filter (fn [f] (re-find #"AuthTree" (.getName f)))))]
      (println f)
      (insert! (with-ids (clojure.edn/read-string (slurp f)))))
    (jdbc/execute! *db* "UPDATE folders SET type='PROJECT_FOLDER' where type IS NULL"))

  (def x (clojure.edn/read-string (slurp "/Users/dlebrero/projects/akvo/gae-to-kafka-initial-import/akvoflow-59.AUUTH.binary.txt")))

  (:org x)
  (first (:auths x))

  (let [root (first (jdbc/query *db* ["select * from folders WHERE name = ? AND flow_id=0" (:org x)]))]
    (assert root (:org x))
    (mapv
      (fn [u]
        (:id (first (jdbc/query *db* ["select * from folders WHERE flow_id=? AND parent_path <@ ?" (:node u) (:parent_path root)]))))
      (:auths x)))

  (doseq [f (->>
              (java.io.File. "/Users/dlebrero/projects/akvo/gae-to-kafka-initial-import/")
              .listFiles
              (filter (fn [f] (re-find #"AUUTH" (.getName f)))))]
    (println f)
    (let [x (clojure.edn/read-string (slurp f))
          root (first (jdbc/query *db* ["select * from folders WHERE name = ? AND flow_id=0" (:org x)]))]
      (assert root (:org x))
      (doseq [u (:auths x)]
        (when-let [new-id (:id (first (jdbc/query *db* ["select * from folders WHERE flow_id=? AND parent_path <@ ?" (:node u) (:parent_path root)])))]
          (jdbc/insert! *db* "user_node_role" {:theuser (str (:theuser u)) :node new-id :role (:role u)})))))

  ;; Not all user auth objects point to an existent folder. Bad data? Example:
  ;{:theuser 14350912, :role 1, :node 17320912}{:org "pacificwash.akvoflow.org"

  (def folders (with-ids {:name "root"
                          :children [{:name "first.level"
                                      :children [{:name "first.first.level"}
                                                 {:name "first.second.level"}]}
                                     {:name "second.level"}]}))
  (insert! folders)
  (def folder-by-name (->>
                        ((fn cc [x]
                           (cons
                             (select-keys x [:id :full-path :name])
                             (mapcat cc (:children x)))) folders)
                        (map (juxt :name identity))
                        (into {})))


  (last (jdbc/query *db* "select * from folders"))
  (jdbc/query *db* ["select * from folders WHERE parent_path <@ ?" (->ltree (:full-path (get folder-by-name "first.first.level")))])
  (jdbc/query *db* ["select * from folders WHERE parent_path @> ?" (->ltree (:full-path (get folder-by-name "first.first.level")))])
  (jdbc/query *db* ["select * from folders WHERE parent_path <@ ?" (->ltree (:full-path (get folder-by-name "first.level")))])
  (jdbc/query *db* ["select * from folders WHERE parent_path @> ?" (->ltree "20063.20132.20133")])

  (jdbc/insert! *db* "user_node_role" {:theuser "dan" :node (:id (get folder-by-name "root")) :role 1})
  (jdbc/insert! *db* "user_node_role" {:theuser "joe" :node (:id (get folder-by-name "first.first.level")) :role 3})
  (jdbc/insert! *db* "user_node_role" {:theuser "joe" :node (:id (get folder-by-name "second.level")) :role 2})
  (jdbc/insert! *db* "user_node_role" {:theuser "max" :node (:id (get folder-by-name "second.level")) :role 2})

  (sort-by :count (jdbc/query *db* "select distinct(theuser),count(*) from user_node_role group by theuser"))
  (jdbc/query *db* "select * from user_node_role where theuser='4000001'")

  (jdbc/query *db* ["select * from folders f,user_node_role u WHERE parent_path @> ? and theuser='dan' and u.node=f.id" (->ltree (:full-path (get folder-by-name "first.first.level")))])
  (jdbc/query *db* ["select * from folders f,user_node_role u WHERE parent_path @> ? and theuser='joe' and u.node=f.id" (->ltree (:full-path (get folder-by-name "first.first.level")))])
  (jdbc/query *db* ["select * from folders f,user_node_role u WHERE parent_path @> ? and theuser='joe' and u.node=f.id" (->ltree (:full-path (get folder-by-name "first.second.level")))])

  (jdbc/query *db* ["select parent_path from folders f,user_node_role u WHERE theuser='dan' and u.node=f.id"])

  (jdbc/query *db* ["WITH\n
                      PERMS AS \n
                      (select parent_path from folders f,user_node_role u WHERE theuser=? and u.node=f.id) \n
                      select * from folders f WHERE nlevel(f.parent_path) <= 2 AND (f.parent_path <@ ARRAY(select * FROM PERMS) OR f.parent_path @> ARRAY(select * FROM PERMS))" "joe"])

  (count (filter (fn [x] (zero? (:flow_id x))) x))

  (jdbc/query *db* ["select * from folders f,user_node_role u WHERE parent_path @> ? and theuser='4000001' and u.node=f.id" (->ltree "19853.19929.19939")])

  (time (def x
          (jdbc/query *db* ["WITH\n
                      PERMS AS \n
                      (select parent_path from folders f,user_node_role u WHERE theuser=? and u.node=f.id) \n
                      select * from folders f WHERE nlevel(f.parent_path) <=2 AND f.parent_path <@ ARRAY(select * FROM PERMS) OR f.parent_path @> ARRAY(select * FROM PERMS)" "4000001"]
            {:explain? true
             :explain-fn (fn [x] (def plan (doall x)))})))
  (clojure.string/join "\n" (map (keyword "query plan") plan))

  (jdbc/query *db* ["WITH
                      PERMS AS
                      (select parent_path from folders f,user_node_role u WHERE theuser=? and u.node=f.id)
                      select * from folders f WHERE f.parent_path <@ ARRAY(select * FROM PERMS) OR f.parent_path @> ARRAY(select * FROM PERMS)" "max"])

  (jdbc/execute! *db* "delete from folders")
  (jdbc/execute! *db* "delete from user_node_role")

  )

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (result-set-read-column [v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (result-set-read-column [v _2 _3]
    (.toLocalTime v))
  Array
  (result-set-read-column [v _ _] (vec (.getArray v)))
  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (parse-string value true)
        "jsonb" (parse-string value true)
        "citext" (str value)
        "ltree" (->ltree value)
        value))))

(defn to-pg-json [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-type clojure.lang.IPersistentVector
  jdbc/ISQLParameter
  (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn (.getConnection stmt)
          meta (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (to-pg-json v))))))

(extend-protocol jdbc/ISQLValue
  java.util.Date
  (sql-value [v]
    (java.sql.Timestamp. (.getTime v)))
  java.time.LocalTime
  (sql-value [v]
    (jt/sql-time v))
  java.time.LocalDate
  (sql-value [v]
    (jt/sql-date v))
  java.time.LocalDateTime
  (sql-value [v]
    (jt/sql-timestamp v))
  java.time.ZonedDateTime
  (sql-value [v]
    (jt/sql-timestamp v))
  ltree
  (sql-value [v]
    (doto (PGobject.)
      (.setType "ltree")
      (.setValue (:v v))))
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  IPersistentVector
  (sql-value [value] (to-pg-json value)))

(defn full-path [node]
  (mapv
    (fn [x] (Long/parseLong x))
    (some-> node
      :parent_path :v
      (clojure.string/split #"\."))))

(defn build-tree [node-list]
  (reduce (fn [tree node]
            (if-let [parent-node (get tree (rules/parent-id node))]
              (-> tree
                (dissoc (:id node))
                (assoc (rules/parent-id node) (update parent-node :children
                                                (fnil conj [])
                                                (get tree (:id node)))))
              tree))
    (into {} (map (juxt :id identity) node-list))
    (sort-by
      (juxt #(- (rules/tree-level %)) :title)
      node-list)))

(defn tree [user-id]
  (->>
    (jdbc/query *db* ["WITH\n
                      PERMS AS \n
                      (select parent_path from folders f,user_node_role u WHERE theuser=? and u.node=f.id) \n
                      select * from folders f WHERE nlevel(f.parent_path) <=3 AND (f.parent_path <@ ARRAY(select * FROM PERMS) OR f.parent_path @> ARRAY(select * FROM PERMS))" user-id])
    (map #(clojure.set/rename-keys % {:name :title}))
    (map #(assoc % :full-path (full-path %)))
    (map #(dissoc % :flow_id :parent_id :parent_path))
    build-tree
    vals
    (sort-by :title)
    vec))

(defn partial-tree [user-id from-node from-level to-level]
  (let [node-path (:parent_path (first (jdbc/query *db* ["select parent_path from folders f WHERE f.id = ?" from-node])))]
    (->>
      (jdbc/query *db* ["WITH\n
                      PERMS AS \n
                      (select parent_path from folders f,user_node_role u WHERE theuser=? and u.node=f.id) \n
                      select * from folders f WHERE f.parent_path <@ ? AND nlevel(f.parent_path) >= ? AND nlevel(f.parent_path) <=? AND (f.parent_path <@ ARRAY(select * FROM PERMS) OR f.parent_path @> ARRAY(select * FROM PERMS))" user-id node-path from-level to-level])
      (map #(clojure.set/rename-keys % {:name :title}))
      (map #(assoc % :full-path (full-path %)))
      (map #(dissoc % :flow_id :parent_id :parent_path))
      (build-tree)
      vals
      (sort-by :title)
      vec
      )))

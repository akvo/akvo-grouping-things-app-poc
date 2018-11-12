(ns user
  (:require [myapp.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [myapp.figwheel :refer [start-fw stop-fw cljs]]
            [myapp.core :refer [start-app]]
            [myapp.db.core]
            [conman.core :as conman]
            [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'myapp.core/repl-server))

(defn stop []
  (mount/stop-except #'myapp.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn restart-db []
  (mount/stop #'myapp.db.core/*db*)
  (mount/start #'myapp.db.core/*db*)
  (binding [*ns* 'myapp.db.core]
    (conman/bind-connection myapp.db.core/*db* "sql/queries.sql")))

(defn reset-db []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration [name]
  (migrations/create name (select-keys env [:database-url])))



(ns myapp.routes.home
  (:require [myapp.layout :as layout]
            [myapp.db.core :as db]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/tree" []
    (response/ok (db/tree "4000001")))
  (GET "/partial-tree" [from-node from-level to-level]
    (response/ok (db/partial-tree "4000001"
                   (Long/parseLong from-node)
                   (Integer/parseInt from-level)
                   (Integer/parseInt to-level))))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8"))))



(ns myapp.handler
  (:require [myapp.middleware :as middleware]
            [myapp.layout :refer [error-page]]
            [myapp.routes.home :refer [home-routes]]
            [myapp.routes.oauth :refer [oauth-routes]]
            [compojure.core :refer [routes wrap-routes]]
            [ring.util.http-response :as response]
            [compojure.route :as route]
            [myapp.env :refer [defaults]]
            [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
    (routes
      (-> #'home-routes
          (wrap-routes middleware/wrap-csrf)
          (wrap-routes middleware/wrap-formats))
          #'oauth-routes
      (route/not-found
        (:body
          (error-page {:status 404
                       :title "page not found"}))))))


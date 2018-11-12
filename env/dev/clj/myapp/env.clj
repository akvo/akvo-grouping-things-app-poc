(ns myapp.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [myapp.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[myapp started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[myapp has shut down successfully]=-"))
   :middleware wrap-dev})

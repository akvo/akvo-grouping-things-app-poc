(ns myapp.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[myapp started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[myapp has shut down successfully]=-"))
   :middleware identity})

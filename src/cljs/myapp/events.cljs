(ns myapp.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [clojure.data :as d]
            [myapp.rules :as rules]))

;;dispatchers

(rf/reg-event-db
  :navigate
  (fn [db [_ page]]
    (assoc db :page page)))

(rf/reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-fx
  :fetch-docs
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/docs"
                  :response-format (ajax/raw-response-format)
                  :on-success [:set-docs]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

;;subscriptions

(rf/reg-sub
  :page
  (fn [db _]
    (:page db)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))

;; mine

(defn tree-updated
  [{:keys [db]} [event new-tree]]
  {:db (assoc db :tree (js->clj new-tree :keywordize-keys true))})

(rf/reg-event-fx :update-tree tree-updated)

(defn whole-tree [db v] (:tree db))

(rf/reg-sub :whole-tree whole-tree)

(rf/reg-event-db
  :set-tree
  (fn [db [_ new-tree]]
    (assoc db :tree
              (js->clj new-tree :keywordize-keys true))))

(rf/reg-event-db
  :set-tree
  (fn [db [_ new-tree]]
    (assoc db :tree
              (js->clj new-tree :keywordize-keys true))))

(rf/reg-event-db
  :set-tree-partial
  (fn [db [_ path new-sub-tree]]
    (-> db
      (update :tree rules/update-path path (fn [n]
                                             (assoc n :subtree-load-state "loaded")))
      (update :tree
        rules/merge-subtree (js->clj new-sub-tree :keywordize-keys true)))))

(rf/reg-event-fx
  :node-toggle
  (fn [{:keys [db]} [event {:keys [treeData node expanded path] :as f}]]
    (when (and
            (= 1 (mod (count path) 3))
            (not (#{"loading" "loaded"} (:subtree-load-state node))))
      {:http-xhrio {:method :get
                    :uri "/partial-tree"
                    :params {:from-node (:id node)
                             :from-level (+ 3 (count path))
                             :to-level (+ 3 (count path))}
                    :format (ajax/transit-request-format)
                    :response-format (ajax/transit-response-format)
                    :on-success [:set-tree-partial path]}
       :db (update db :tree rules/update-path path (fn [n]
                                                     (update n :subtree-load-state
                                                       (fn [current-state]
                                                         (or current-state "loading")))))})))

(rf/reg-event-fx
  :fetch-tree
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/tree"
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:set-tree]}}))

(ns myapp.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]))

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
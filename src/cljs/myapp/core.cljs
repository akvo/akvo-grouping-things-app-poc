(ns myapp.core
  (:require [baking-soda.core :as b]
            [day8.re-frame.http-fx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [myapp.ajax :as ajax]
            [myapp.events]
            [secretary.core :as secretary]
            [sortable-tree :as st])
  (:import goog.History))

; the navbar components are implemented via baking-soda [1]
; library that provides a ClojureScript interface for Reactstrap [2]
; Bootstrap 4 components.
; [1] https://github.com/gadfly361/baking-soda
; [2] http://reactstrap.github.io/

(defn nav-link [uri title page]
  [b/NavItem
   [b/NavLink
    {:href uri
     :active (when (= page @(rf/subscribe [:page])) "active")}
    title]])

(defn navbar []
  (r/with-let [expanded? (r/atom true)]
    [b/Navbar {:light true
               :class-name "navbar-dark bg-primary"
               :expand "md"}
     [b/NavbarBrand {:href "/"} "myapp"]
     [b/NavbarToggler {:on-click #(swap! expanded? not)}]
     [b/Collapse {:is-open @expanded? :navbar true}
      [b/Nav {:class-name "mr-auto" :navbar true}
       [nav-link "#/" "Home" :home]]]]))

(defn home-page []
  [:div.container
   (let [tree (rf/subscribe [:whole-tree])]
     [(r/adapt-react-class st) {:tree-data @tree

                                :style {:height "800px"}
                                :can-node-have-children (fn [node]
                                                          (= "PROJECT_FOLDER" (.-type node)))
                                :generate-node-props (fn [o]
                                                       (let [node (.-node o)]
                                                         (clj->js {:style {:boxShadow "0 0 0 4px"}
                                                                   :buttons [(r/as-element [b/Button {:color "Danger"} "ho"])
                                                                             (r/as-element [b/Badge {} "jo"])]})))
                                :get-node-key (fn [node]
                                                (.-id (.-node node)))
                                :on-visibility-toggle (fn [event]
                                                        (rf/dispatch [:node-toggle (js->clj event :keywordize-keys true)]))
                                :on-change #(rf/dispatch [:update-tree %])}])])

(def pages
  {:home #'home-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes

(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:navigate :home]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))


#_(rf/reg-event-db
    :initialise-db
    (fn [_ _]
      {:tree [{:title "foo"
               :type "folder"
               :children [{:title "bar"
                           :type "folder"
                           :children [{:title "bzzzz"
                                       :type "survey"}]}]}]}))

(defn init! []
  ;(rf/dispatch-sync [:initialise-db])
  (rf/dispatch-sync [:navigate :home])
  (ajax/load-interceptors!)
  (rf/dispatch [:fetch-tree])
  (hook-browser-navigation!)
  (mount-components))

(ns myapp.perms
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [sortable-tree :as st]))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]
     (let [tree (rf/subscribe [:whole-tree])]
       [(r/adapt-react-class st) {:tree-data @tree
                                  :can-node-have-children (fn [node]
                                                            (=
                                                              "folder"
                                                              (:type (js->clj node :keywordize-keys true))))
                                  :on-change #(rf/dispatch [:update-tree %])}])]]])
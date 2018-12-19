(ns myapp.rules)

; For a given user, draw everything he can see, up to some depth
; From a given path and user, draw everything he has see, up to some depth
; For a given user, can he edit/see a given path
; List all objects in a path/folder

; to see a dashboard, you need to be able to see all the forms in that dashboard OR the dashboard has user specific perms

;; User can have perms in a given node or a parent node
;; Node has only one parent

;; node hierarchy
;; perms to nodes

;; cache
;; OR keep ancestors updated

;{:id :root
; :u {:client1 #{:role1 :role2}}
; :c [{:id :first
;      :u {:client2 #{:role3}}}
;     {:id :second}]}
;
;{:role1 #{:read :write}
; :role2 #{:read}}
;
;[:root [[:first [[:ffirst []]
;                 [:fnext []]]]
;        [:second []]]]
;
;:user -> [[:node :role],,,]
;:role -> #{:perm}
;:perm -> #{:project
;           :form
;           :data
;           :device
;           :cascade}                                        ;; crud on these
;
;[:node :user :perm]
;

;"root.first.ffirst"

; move first under second
;"root.second.first" -> replace "root.first" to "root.second.first"
;"root.second.first" -> replace "root.first" to "root.second.first", inc level

; find two levels under second ->
;"root.second.*.*" OR "root.second.*"
;"root.second%" && (level == 3 or 4)

; find what the client can see in the first two levels

;node -*-> node
;node -|-> user
;      |
;     role -*-> perms

; can the user edit node?
;get-node "root.second.first.ffirst"
;(or root, root.second, root.second.first ...)

(defn parent-path [node]
  (butlast (:full-path node)))

(defn parent-id [node]
  (-> node parent-path last))

(defn tree-level [node]
  (-> node :full-path count))

(defn update-path [tree-data [id & path] f]
  (mapv
    (fn [x]
      (if (= id (:id x))
        (if (seq path)
          (update x :children update-path path f)
          (f x))
        x))
    tree-data))

(defn merge-subtree [tree subtrees]
  (reduce
    (fn [final-tree subtree]
      (update-path final-tree (parent-path subtree) (fn [parent-node]
                                                      (update parent-node :children
                                                        (fnil conj []) subtree))))
    tree
    subtrees))
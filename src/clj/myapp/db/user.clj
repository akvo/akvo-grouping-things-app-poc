(ns myapp.db.user
  (:require
    [myapp.db.core :as db]
    [cheshire.core :refer [generate-string parse-string]]
    [clojure.java.jdbc :as jdbc]
    [conman.core :as conman]
    [clojure.set :as set]))

(conman/bind-connection db/*db* "sql/user.sql")
(conman/bind-connection db/*db* "sql/tree.sql")

(defn get-node-by-flow-id- [flow-id]
  (some->
    (get-node-by-flow-id {:flow-id flow-id})
    (set/rename-keys {:flow_id :flow-id
                      :parent_path :parent-path})
    (update :parent-path :v)))

(defn upsert-role [{:keys [flow-id name permissions]}]
  (jdbc/with-db-transaction [tx db/*db*]
    (let [{:keys [id]} (upsert-role! tx {:flow-id flow-id :name name})]
      (delete-role-perms-for-role! tx {:id id})
      (create-role-perms! tx {:permissions (map (fn [p] [id p]) permissions)}))))

(defn upsert-user-auth [{:keys [userId roleId securedObjectId flow-id]}]
  (let [{:keys [email]} (get-user-by-flow-id {:flow-id userId})
        {role-id :id} (get-role-by-flow-id {:flow-id roleId})
        {node-id :id} (get-node-by-flow-id- securedObjectId)]
    (assert email (str "Invalid user" userId))
    (assert role-id (str "Invalid role" roleId))
    (assert node-id (str "Invalid securedObject" securedObjectId))
    (upsert-user-auth! {:flow-id flow-id :theuser email :role role-id :node node-id})))

(defn upsert-node [{:keys [name projectType parentId flow-id]}]
  (if (zero? parentId)
    (if-let [node (get-node-by-flow-id- flow-id)]
      ;; TODO: if the parent has changed, update all childs of this node to reflect the new parent path
      (update-node! {:name name
                     :type projectType
                     :id (:id node)
                     :parent-path (db/->ltree (str (:id node)))})
      (let [new-id (db/next-id)]
        (insert-node! {:id new-id
                       :flow-id flow-id
                       :name name
                       :type projectType
                       :parent-path (db/->ltree (str new-id))})))
    (let [parent-node (get-node-by-flow-id- parentId)]
      (assert parent-node)
      (if-let [node (get-node-by-flow-id- flow-id)]
        (update-node! {:name name
                       :type projectType
                       :id (:id node)
                       :parent-path (db/->ltree (str (:parent-path parent-node) "." (:id node)))})
        (let [new-id (db/next-id)]
          (insert-node! {:id new-id
                         :flow-id flow-id
                         :name name
                         :type projectType
                         :parent-path (db/->ltree (str (:parent-path parent-node) "." new-id))}))))))

(comment
  (get-user-by-flow-id {:flow-id 123})
  (get-role-by-flow-id {:flow-id 2003})
  (jdbc/query db/*db* "select * from user_node_role")

  (upsert-user! {:flow-id 123 :email "hey2@goo.com"})
  )

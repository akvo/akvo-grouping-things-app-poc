-- :name upsert-user! :! :n
INSERT INTO users (flow_id, email)
VALUES
 (
 :flow-id,
 :email
 )
ON CONFLICT (flow_id)
DO
 UPDATE
   SET email = :email;

-- :name get-user-by-flow-id :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE flow_id = :flow-id

-- :name upsert-role! :i :1
INSERT INTO roles (flow_id, name)
VALUES
 (
 :flow-id,
 :name
 )
ON CONFLICT (flow_id)
DO
 UPDATE
   SET name = :name
RETURNING id

-- :name create-role-perms! :! :n
insert into role_perms (role, perm)
values :tuple*:permissions

-- :name delete-role-perms-for-role! :! :n
DELETE FROM role_perms WHERE role = :id

-- :name get-role-by-flow-id :? :1
-- :doc retrieves a user record given the id
SELECT * FROM roles
WHERE flow_id = :flow-id

-- :name upsert-user-auth! :!
INSERT INTO user_node_role (theuser, flow_id, node, role)
VALUES
 (
 :theuser,
 :flow-id,
 :node,
 :role
 )
ON CONFLICT (flow_id)
DO
 UPDATE
   SET theuser = :theuser,
       node = :node,
       role = :role

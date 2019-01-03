-- :name get-node-by-flow-id :? :1
SELECT * FROM folders
WHERE flow_id = :flow-id

-- :name insert-node! :!
INSERT INTO folders (id, name, type, flow_id, parent_path)
VALUES (:id, :name, :type, :flow-id, :parent-path)

-- :name update-node! :!
UPDATE folders
SET name=:name,
    type=:type,
    parent_path=:parent-path
WHERE id=:id

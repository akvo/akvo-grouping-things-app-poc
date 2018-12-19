CREATE TABLE folders (
  id SERIAL PRIMARY KEY,
  name varchar(2000),
  type varchar(20),
  flow_id bigint,
  parent_id INTEGER REFERENCES folders,
  parent_path ltree);
--;;
CREATE INDEX folders_parent_path_idx ON folders USING GIST (parent_path);
--;;
CREATE INDEX folders_parent_id_idx ON folders (parent_id);
--;;
CREATE TABLE roles (
  id SERIAL PRIMARY KEY,
  name varchar(200));
--;;
CREATE TABLE role_perms (
  role INTEGER REFERENCES roles,
  perm varchar(30));
--;;
CREATE TABLE user_node_role (
  theuser varchar(200),
  node INTEGER REFERENCES folders,
  role INTEGER REFERENCES roles);
--;;
CREATE SEQUENCE folder_id;
--;;
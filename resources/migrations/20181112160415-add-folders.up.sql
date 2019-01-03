CREATE TABLE folders (
  id SERIAL PRIMARY KEY,
  name varchar(2000),
  type varchar(20) NOT NULL,
  flow_id bigint UNIQUE NOT NULL,
  parent_path ltree NOT NULL);
--;;
CREATE INDEX folders_parent_path_idx ON folders USING GIST (parent_path);
--;;
CREATE TABLE roles (
  id SERIAL PRIMARY KEY,
  flow_id bigint UNIQUE NOT NULL,
  name varchar(200));
--;;
CREATE TABLE role_perms (
  role INTEGER REFERENCES roles NOT NULL,
  perm varchar(30) NOT NULL);
--;;
CREATE TABLE user_node_role (
  theuser varchar(200) NOT NULL,
  flow_id bigint UNIQUE NOT NULL,
  node INTEGER REFERENCES folders NOT NULL,
  role INTEGER REFERENCES roles NOT NULL);
--;;
CREATE SEQUENCE folder_id;
--;;
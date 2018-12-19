insert into roles(name)
values ('admin'),
       ('enumerator'),
       ('read-only')
--;;
insert into role_perms(role,perm)
values ('1', 'ANYTHING'),
       ('2', 'READ_SOMETHING'),
       ('2', 'WRITE_SURVEY'),
       ('3', 'READ_ONLY')
drop table version_db;

alter table calls drop constraint fk_call_env_id;

alter table completion_statuses drop constraint FKE74D8D23676825D9;

alter table jobs drop constraint FK31DC5669A022E1;

alter table log_directories drop constraint FK48714350676825D9;

alter table log_files drop constraint FK880E3DBC676825D9;

drop table calls;

drop table completion_statuses;

drop table environments;

drop table jobs;

drop table log_directories;

drop table log_files;


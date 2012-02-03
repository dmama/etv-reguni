-- table VERSION
create table version_db (version_nb varchar(10) not null, script_id varchar(50) not null, ts timestamp default now());
insert into version_db (version_nb, script_id) values ('1.0', 'create');

create table calls (id  bigserial not null, caller varchar(20), latency int8, method varchar(50), params text, service varchar(20), date timestamp, env_id int8 not null, primary key (id));

create table completion_statuses (id  bigserial not null, up_to timestamp, env_id int8 not null, primary key (id));

create table environments (id  bigserial not null, name varchar(30), primary key (id));

create table jobs (DTYPE varchar(31) not null, id  bigserial not null, cron varchar(30), name varchar(30), logdir_id int8 not null, primary key (id));

create table log_directories (id  bigserial not null, directory_path varchar(255), pattern varchar(100), env_id int8 not null, primary key (id));

create table log_files (id  bigserial not null, filepath varchar(255), env_id int8 not null, primary key (id));

create index ids_calls_date on calls (date);

create index ids_calls_method on calls (method);

create index idx_calls_caller on calls (caller);

create index idx_calls_env on calls (env_id);

create index ids_calls_service on calls (service);

alter table calls add constraint fk_call_env_id foreign key (env_id) references environments;

create index idx_cstatus_env on completion_statuses (env_id);

alter table completion_statuses add constraint FKE74D8D23676825D9 foreign key (env_id) references environments;

create index idx_job_logdir on jobs (logdir_id);

alter table jobs add constraint FK31DC5669A022E1 foreign key (logdir_id) references log_directories;

create index idx_logdir_env on log_directories (env_id);

alter table log_directories add constraint FK48714350676825D9 foreign key (env_id) references environments;

create index idx_logfile_env on log_files (env_id);

alter table log_files add constraint FK880E3DBC676825D9 foreign key (env_id) references environments;


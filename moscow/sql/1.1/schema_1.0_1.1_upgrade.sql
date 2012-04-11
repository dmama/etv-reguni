-- version
insert into version_db (version_nb, script_id) values ('1.1', '1.0_1.1_upgrade');

-- factorize some data from the calls table into satellite tables
create table callers (id  bigserial not null, name varchar(30), primary key (id));
create table methods (id  bigserial not null, name varchar(60), primary key (id));
create table services (id  bigserial not null, name varchar(30), primary key (id));

insert into callers(name) select distinct caller from calls order by caller;
insert into methods(name) select distinct method from calls order by method;
insert into services(name) select distinct service from calls order by service;

alter table calls add column caller_id int8;
update calls set caller_id = s.id from callers s where s.name = caller;
alter table calls alter column caller_id set not null;
alter table calls add constraint fk_calls_caller_id foreign key (caller_id) references callers;
create index idx_calls_caller_id on calls (caller_id);

alter table calls add column method_id int8;
update calls set method_id = m.id from methods m where m.name = method;
alter table calls alter column method_id set not null;
alter table calls add constraint fk_calls_method_id foreign key (method_id) references methods;
create index idx_calls_method_id on calls (method_id);

alter table calls add column service_id int8;
update calls set service_id = s.id from services s where s.name = service;
alter table calls alter column service_id set not null;
alter table calls add constraint fk_calls_service_id foreign key (service_id) references services;
create index idx_calls_service_id on calls (service_id);

-- now that data is factorized, drop the corresponding columns
alter table calls drop column caller;
alter table calls drop column method;
alter table calls drop column service;
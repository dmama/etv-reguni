-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('3.4.2', '3.4_3.4.2_upgrade');

-- Table TACHE
ALTER TABLE TACHE add (QUALIFICATION nvarchar2(16));
create index IDX_TACHE_CTB_ID on TACHE (CTB_ID);

ALTER TABLE TACHE add (CA_ID number(19,0));
alter table TACHE add constraint FK_TACH_CA_ID foreign key (CA_ID) references TIERS;
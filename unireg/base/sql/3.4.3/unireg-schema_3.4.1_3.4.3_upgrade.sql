-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('3.4.3', '3.4_1_3.4.3_upgrade');

-- Table TACHE
ALTER TABLE TACHE add (QUALIFICATION nvarchar2(16));
create index IDX_TACHE_CTB_ID on TACHE (CTB_ID);

ALTER TABLE TACHE add (CA_ID number(19,0));
alter table TACHE add constraint FK_TACH_CA_ID foreign key (CA_ID) references TIERS;

-- Adresse de retour des DIs
ALTER TABLE TACHE add (DECL_ADRESSE_RETOUR nvarchar2(4));
ALTER TABLE DECLARATION add (DELAI_RETOUR_IMPRIME number(10,0));

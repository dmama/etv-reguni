-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.5.0', '4.4.0_4.5.0_upgrade');

--
-- [UNIREG-1059] Déplacé les remarques dans une table satellite
--
create table REMARQUE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), TEXTE nvarchar2(2000), TIERS_ID number(19,0) not null, primary key (id));
alter table REMARQUE add constraint FK_REMARQUE_TRS_ID foreign key (TIERS_ID) references TIERS;

insert into REMARQUE(ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, TEXTE, TIERS_ID)
select hibernate_sequence.nextval, CURRENT_DATE, '[reprise initiale]', CURRENT_DATE, '[reprise initiale]', t.REMARQUE, t.NUMERO from TIERS t where t.REMARQUE is not null;

create index IDX_REMARQUE_TIERS_ID on REMARQUE (TIERS_ID);

--
--[UNIREG-2138] Historisation de la periodicite du débiteur
--
create table PERIODICITE (PERIODICITE_TYPE nvarchar2(31) not null,id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65),DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0) ,PERIODE_DECOMPTE nvarchar2(3), DEBITEUR_ID number(19,0) not null, primary key (id));

alter table PERIODICITE add constraint FK_PERIODICITE_DB_ID foreign key (DEBITEUR_ID) references TIERS;

create index IDX_P_DEBITEUR_ID on PERIODICITE (DEBITEUR_ID);

--
-- [UNIREG-2399] Fusion des événements civils unitaires et regroupés - cleanup
--
drop table EVENEMENT_CIVIL_UNITAIRE;

--
-- [UNIREG-1979] Ajout de la fonctionnalité de schedule de réindexation dans le futur
--
ALTER TABLE TIERS add (REINDEX_ON number(10,0));

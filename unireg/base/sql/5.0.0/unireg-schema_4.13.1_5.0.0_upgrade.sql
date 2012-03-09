-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.0.0', '4.13.1_5.0.0_upgrade');

--
-- Création de la nouvelle structure de données pour les événements civils e-CH
--
create table EVENEMENT_CIVIL_ECH (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), ACTION_EVT nvarchar2(20) not null, COMMENTAIRE_TRAITEMENT nvarchar2(255), DATE_EVENEMENT number(10,0) not null, DATE_TRAITEMENT timestamp, ETAT nvarchar2(10) not null, NO_INDIVIDU number(19,0), REF_MESSAGE_ID number(19,0), TYPE nvarchar2(40) not null, primary key (id));
create index IDX_EV_CIV_ECH_ETAT on EVENEMENT_CIVIL_ECH (ETAT);
create index IDX_EV_CIV_ECH_NO_IND on EVENEMENT_CIVIL_ECH (NO_INDIVIDU);

create table EVENEMENT_CIVIL_ECH_ERREUR (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), CALLSTACK nvarchar2(2000), MESSAGE nvarchar2(1024), TYPE nvarchar2(7) not null, EVT_CIVIL_ID number(19,0) not null, primary key (id));
create index IDX_EV_ECH_ERR_EV_ID on EVENEMENT_CIVIL_ECH_ERREUR (EVT_CIVIL_ID);
alter table EVENEMENT_CIVIL_ECH_ERREUR add constraint FK_EV_ERR_EV_ECH_ID foreign key (EVT_CIVIL_ID) references EVENEMENT_CIVIL_ECH;

-- Suppression des numéros de tiers dans les tables d'événements civils
alter table EVENEMENT_CIVIL drop column HAB_PRINCIPAL;
alter table EVENEMENT_CIVIL drop column HAB_CONJOINT;

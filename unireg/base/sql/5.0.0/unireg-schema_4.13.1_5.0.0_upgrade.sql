-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.0.0', '4.13.1_5.0.0_upgrade');

--
-- Création de la nouvelle structure de données pour les événements civils e-CH
--
create table EVENEMENT_CIVIL_ECH (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), COMMENTAIRE_TRAITEMENT nvarchar2(255), DATE_EVENEMENT number(10,0) NOT NULL, DATE_TRAITEMENT timestamp, ETAT nvarchar2(10) NOT NULL, PP_ID number(19,0), NO_INDIVIDU number(19,0), TYPE nvarchar2(40) NOT NULL, ACTION_EVT nvarchar2(20), primary key (id));
create index IDX_EV_CIV_ECH_ETAT on EVENEMENT_CIVIL_ECH (ETAT);
create index IDX_EV_CIV_ECH_NO_IND on EVENEMENT_CIVIL_ECH (NO_INDIVIDU);
create index IDX_EV_CIV_ECH_PP on EVENEMENT_CIVIL_ECH (PP_ID);

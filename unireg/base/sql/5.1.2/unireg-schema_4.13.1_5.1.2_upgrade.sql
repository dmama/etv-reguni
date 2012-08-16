-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.1.2', '4.13.1_5.1.2_upgrade');

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

-- [SIFISC-4560] passage de number(10,0) à number(19,0) de la colonne AUDIT_LOG.EVT_ID
alter table AUDIT_LOG modify (EVT_ID number(19,0));

-- SIFISC-4526: Les requêtes SQL pour obtenir la liste des utilisateurs, des priorités et des types de messages prennent >2 secondes
create index IDX_EVT_IDENT_CTB_TRAIT_USER on EVENEMENT_IDENTIFICATION_CTB (TRAITEMENT_USER);
create index IDX_EVT_IDENT_CTB_ETAT on EVENEMENT_IDENTIFICATION_CTB (ETAT);

-- e-Facture
ALTER TABLE TIERS ADD (ADRESSE_EMAIL_EFACTURE nvarchar2(255));

-- [SIFISC-5349]
ALTER TABLE TIERS MODIFY (NH_CAT_ETRANGER NVARCHAR2(50));
update tiers set NH_CAT_ETRANGER = '_04_CONJOINT_DIPLOMATE_OU_FONCT_INT_CI' where NH_CAT_ETRANGER = '_04_CONJOINT_DIPLOMATE_CI';
update tiers set NH_CAT_ETRANGER = '_11_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL' where NH_CAT_ETRANGER in ('_11_DIPLOMATE', '_12_FONCTIONNAIRE_INTERNATIONAL');
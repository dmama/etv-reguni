-- partie 2.7.7 -> 3.2
alter table DECLARATION add (SANS_RAPPEL decimal(1));
alter table TIERS add (NH_NO_OFS_COMMUNE_ORIGINE number(10,0));
alter table TIERS rename column NH_NO_OFS_PAYS_ORIGINE to NH_NO_OFS_NATIONALITE;
create index IDX_ANC_NO_SRC on TIERS (ANCIEN_NUMERO_SOURCIER);

-- partie 3.2 -> 3.2.1
alter table DECLARATION modify TIERS_ID not null;

alter table DELAI_DECLARATION modify DECLARATION_ID not null;

--TODO (msi/jde/gdy) vérifier que les état avec fk nulles en prod correspondent bien aux DIs supprimées par le patch de correction
delete from ETAT_DECLARATION where DECLARATION_ID is null;

alter table ETAT_DECLARATION modify DECLARATION_ID not null;

-- partie 3.2.1 -> 3.3
alter table ADRESSE_TIERS modify TIERS_ID not null;

alter table DROIT_ACCES modify TIERS_ID not null;

alter table FOR_FISCAL modify TIERS_ID not null;

delete from IDENTIFICATION_PERSONNE where NON_HABITANT_ID is null;
alter table IDENTIFICATION_PERSONNE modify NON_HABITANT_ID not null;

alter table RAPPORT_ENTRE_TIERS modify TIERS_OBJET_ID not null;

alter table RAPPORT_ENTRE_TIERS modify TIERS_SUJET_ID not null;

alter table SITUATION_FAMILLE modify CTB_ID not null;

delete from EVENEMENT_CIVIL_ERREUR where EVT_CIVIL_ID is null;
alter table EVENEMENT_CIVIL_ERREUR modify EVT_CIVIL_ID not null;

-- table VERSION
CREATE TABLE VERSION_DB (VERSION_NB VARCHAR2(10 CHAR) NOT NULL, SCRIPT_ID VARCHAR2(50 CHAR) NOT NULL, TS TIMESTAMP DEFAULT SYSDATE);
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('3.3', '2.8_3.3_upgrade');

create table EVENEMENT_IDENTIFICATION_CTB (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER varchar2(65 char), LOG_CDATE timestamp, LOG_CUSER varchar2(65 char), LOG_MDATE timestamp, LOG_MUSER varchar2(65 char), DATE_DEMANDE timestamp, EMETTEUR_ID varchar2(50 char) not null, MESSAGE_ID varchar2(36 char) not null, PERIODE_FISCALE number(10,0) not null, NAVS11 varchar2(11 char), NAVS13 varchar2(13 char), ADR_CH_COMPL varchar2(2 char), ADR_CODE_PAYS varchar2(12 char), ADR_LIEU varchar2(40 char), ADR_LIGNE_1 varchar2(60 char), ADR_LIGNE_2 varchar2(60 char), ADR_LOCALITE varchar2(40 char), ADR_NO_APPART varchar2(10 char), ADR_ORDRE_POSTE number(10,0), ADR_NO_POLICE varchar2(12 char), ADR_NPA_ETRANGER varchar2(15 char), ADR_NPA_SUISSE number(10,0), ADR_NO_CP number(10,0), ADR_RUE varchar2(60 char), ADR_TEXT_CP varchar2(15 char), ADR_TYPE varchar2(255 char), DATE_NAISSANCE number(10,0), NOM varchar2(100 char) not null, PRENOMS varchar2(100 char) not null, SEXE varchar2(8 char), PRIO_EMETTEUR varchar2(255 char) not null, PRIO_UTILISATEUR number(10,0) not null, TYPE_MESSAGE varchar2(20 char) not null, UTILISATEUR_ID varchar2(50 char) not null, ETAT varchar2(23 char), BUSINESS_ID varchar2(36 char) not null, BUSINESS_USER varchar2(36 char) not null, REPLY_TO varchar2(50 char) not null, NB_CTB_TROUVES number(10,0), DATE_REPONSE timestamp, ERREUR_CODE varchar2(20 char), ERREUR_MESSAGE varchar2(1000 char), ERREUR_TYPE varchar2(9 char), NO_CONTRIBUABLE number(19,0), primary key (id));

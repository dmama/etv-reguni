-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.13.1', '4.13.0_4.13.1_upgrade');

--
-- [SIFISC-3156] Nouveau format de la table immeuble
--
drop table IMMEUBLE cascade constraints;
create table IMMEUBLE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0), DATE_DERNIERE_MUTATION number(10,0), DATE_FIN number(10,0), DATE_VALID_RF number(10,0), DERNIERE_MUTATION nvarchar2(28), ESTIMATION_FISCALE number(10,0), GENRE_PROPRIETE nvarchar2(12) not null, ID_RF nvarchar2(40) not null, LIEN_RF nvarchar2(500), NATURE_IMMEUBLE nvarchar2(255), NOM_COMMUNE nvarchar2(255) not null, NUMERO_IMMEUBLE nvarchar2(20) not null, PART_PROPRIETE_DENOMINATEUR number(10,0) not null, PART_PROPRIETE_NUMERATEUR number(10,0) not null, ID_PROPRIETAIRE_RF nvarchar2(40) not null, NUMERO_INDIVIDU_RF number(19,0) not null, REF_ESTIM_FISC nvarchar2(255), TYPE_IMMEUBLE nvarchar2(27) not null, CTB_ID number(19,0) not null, primary key (id));
create index IDX_IMM_CTB_ID on IMMEUBLE (CTB_ID);
alter table IMMEUBLE add constraint FK_IMM_CTB_ID foreign key (CTB_ID) references TIERS;

--
-- [SIFISC-143] Ajout de la colonne NPA_CASE_POSTALE
--
ALTER TABLE ADRESSE_TIERS ADD (NPA_CASE_POSTALE number(10, 0));

--
-- [SIFISC-3845] modification d ela longeur du champ messageID
--
alter table EVENEMENT_IDENTIFICATION_CTB modify (MESSAGE_ID nvarchar2(255));



--
-- Script de purge des données de RF
--

delete from AUTRE_DOCUMENT_FISCAL where DD_IMMEUBLE_ID is not null;
ALTER TABLE AUTRE_DOCUMENT_FISCAL DROP CONSTRAINT FK_DD_RF_IMMEUBLE_ID;

drop table ALLEGEMENT_FONCIER cascade constraints;
drop table RAPPROCHEMENT_RF cascade constraints;

drop table RF_SURFACE_TOTALE cascade constraints;
drop table RF_SURFACE_AU_SOL cascade constraints;
drop table RF_SITUATION cascade constraints;
drop table RF_IMPLANTATION cascade constraints;
drop table RF_ESTIMATION cascade constraints;
drop table RF_DROIT cascade constraints;
drop table RF_IMMEUBLE cascade constraints;
drop table RF_DESCRIPTION_BATIMENT cascade constraints;
drop table RF_BATIMENT cascade constraints;
drop table RF_AYANT_DROIT cascade constraints;
drop table RF_COMMUNE cascade constraints;

drop table EVENEMENT_RF_IMPORT cascade constraints;
drop table EVENEMENT_RF_MUTATION cascade constraints;

CREATE TABLE EVENEMENT_RF_IMPORT
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    CALLSTACK CLOB,
    DATE_EVENEMENT NUMBER(10),
    ERROR_MESSAGE NVARCHAR2(1000),
    ETAT NVARCHAR2(13),
    FILE_URL NVARCHAR2(512)
);
CREATE TABLE EVENEMENT_RF_MUTATION
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    CALLSTACK CLOB,
    ERROR_MESSAGE NVARCHAR2(1000),
    ETAT NVARCHAR2(9),
    ID_RF NVARCHAR2(33),
    TYPE_ENTITE NVARCHAR2(14) NOT NULL,
    TYPE_MUTATION NVARCHAR2(13) NOT NULL,
    XML_CONTENT CLOB,
    IMPORT_ID NUMBER(19) NOT NULL
);
CREATE INDEX IDX_EV_RF_IMP_ETAT ON EVENEMENT_RF_IMPORT (ETAT);
ALTER TABLE EVENEMENT_RF_MUTATION ADD FOREIGN KEY (IMPORT_ID) REFERENCES EVENEMENT_RF_IMPORT (ID);
CREATE INDEX IDX_EV_RF_MUT_ETAT ON EVENEMENT_RF_MUTATION (ETAT);
CREATE INDEX IDX_EV_RF_ID_RF ON EVENEMENT_RF_MUTATION (ID_RF);
CREATE INDEX IDX_EV_RF_MUT_TYPE_ENTITE ON EVENEMENT_RF_MUTATION (TYPE_ENTITE);
CREATE INDEX IDX_EV_RF_IMP_ID ON EVENEMENT_RF_MUTATION (IMPORT_ID);

-- Tables pour les données du registre foncier
CREATE TABLE RF_AYANT_DROIT
(
    TYPE NVARCHAR2(31) NOT NULL,
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    ID_RF NVARCHAR2(33) NOT NULL,
    NO_CTB NUMBER(19),
    NO_RF NUMBER(19),
    RAISON_SOCIALE NVARCHAR2(255),
    TYPE_COMMUNAUTE NVARCHAR2(22),
    NUMERO_RC NVARCHAR2(20),
    DATE_NAISSANCE NUMBER(10),
    NOM_PP NVARCHAR2(250),
    PRENOM_PP NVARCHAR2(250)
);
CREATE TABLE RF_BATIMENT
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    MASTER_ID_RF NVARCHAR2(33) NOT NULL
);
CREATE TABLE RF_COMMUNE
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    NO_OFS NUMBER(10) NOT NULL,
    NO_RF NUMBER(10) NOT NULL,
    NOM_RF NVARCHAR2(50) NOT NULL
);
CREATE TABLE RF_DESCRIPTION_BATIMENT
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    SURFACE NUMBER(10),
    TYPE NVARCHAR2(255),
    BATIMENT_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_DROIT
(
    TYPE NVARCHAR2(31) NOT NULL,
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    DATE_DEBUT_OFFICIELLE NUMBER(10),
    MASTER_ID_RF NVARCHAR2(33) NOT NULL,
    MOTIF_DEBUT_CODE NVARCHAR2(255),
    MOTIF_FIN_CODE NVARCHAR2(255),
    NO_AFFAIRE NVARCHAR2(40),
    IDENTIFIANT_DROIT NVARCHAR2(15),
    PART_PROP_DENOM NUMBER(10),
    PART_PROP_NUM NUMBER(10),
    REGIME_PROPRIETE NVARCHAR2(12),
    AYANT_DROIT_ID NUMBER(19) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL,
    COMMUNAUTE_ID NUMBER(19)
);
CREATE TABLE RF_ESTIMATION
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    DATE_INSCRIPTION NUMBER(10),
    EN_REVISION NUMBER(1) NOT NULL,
    MONTANT NUMBER(19),
    REFERENCE NVARCHAR2(25),
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_IMMEUBLE
(
    TYPE NVARCHAR2(31) NOT NULL,
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_RADIATION NUMBER(10),
    EGRID NVARCHAR2(14),
    ID_RF NVARCHAR2(33) NOT NULL,
    URL_INTERCAPI NVARCHAR2(2000),
    CFA NUMBER(1),
    QUOTE_PART_DENOM NUMBER(10),
    QUOTE_PART_NUM NUMBER(10)
);
CREATE TABLE RF_IMPLANTATION
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    SURFACE NUMBER(10),
    BATIMENT_ID NUMBER(19) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_SITUATION
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    INDEX1 NUMBER(10),
    INDEX2 NUMBER(10),
    INDEX3 NUMBER(10),
    NO_PARCELLE NUMBER(10) NOT NULL,
    COMMUNE_ID NUMBER(19) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_SURFACE_AU_SOL
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    SURFACE NUMBER(10) NOT NULL,
    TYPE NVARCHAR2(250) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_SURFACE_TOTALE
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    SURFACE NUMBER(10) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE UNIQUE INDEX IDX_AYANTDROIT_ID_RF ON RF_AYANT_DROIT (ID_RF);
CREATE UNIQUE INDEX IDX_BATIMENT_MASTER_ID_RF ON RF_BATIMENT (MASTER_ID_RF);
CREATE UNIQUE INDEX IDX_COMMUNE_NO_OFS ON RF_COMMUNE (NO_OFS);
ALTER TABLE RF_DESCRIPTION_BATIMENT ADD FOREIGN KEY (BATIMENT_ID) REFERENCES RF_BATIMENT (ID);
CREATE INDEX IDX_DESCR_BAT_RF_BATIMENT_ID ON RF_DESCRIPTION_BATIMENT (BATIMENT_ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (AYANT_DROIT_ID) REFERENCES RF_AYANT_DROIT (ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (COMMUNAUTE_ID) REFERENCES RF_AYANT_DROIT (ID);
CREATE INDEX IDX_DROIT_MASTER_ID_RF ON RF_DROIT (MASTER_ID_RF);
CREATE INDEX IDX_DROIT_RF_AYANT_DROIT_ID ON RF_DROIT (AYANT_DROIT_ID);
CREATE INDEX IDX_DROIT_RF_IMMEUBLE_ID ON RF_DROIT (IMMEUBLE_ID);
CREATE INDEX IDX_DROIT_RF_COMM_ID ON RF_DROIT (COMMUNAUTE_ID);
ALTER TABLE RF_ESTIMATION ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_ESTIM_RF_IMMEUBLE_ID ON RF_ESTIMATION (IMMEUBLE_ID);
CREATE UNIQUE INDEX IDX_IMMEUBLE_RF_ID ON RF_IMMEUBLE (ID_RF);
ALTER TABLE RF_IMPLANTATION ADD FOREIGN KEY (BATIMENT_ID) REFERENCES RF_BATIMENT (ID);
ALTER TABLE RF_IMPLANTATION ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_IMPLANT_RF_BATIMENT_ID ON RF_IMPLANTATION (BATIMENT_ID);
CREATE INDEX IDX_IMPLANT_RF_IMMEUBLE_ID ON RF_IMPLANTATION (IMMEUBLE_ID);
ALTER TABLE RF_SITUATION ADD FOREIGN KEY (COMMUNE_ID) REFERENCES RF_COMMUNE (ID);
ALTER TABLE RF_SITUATION ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_SITUATION_RF_COMMUNE_ID ON RF_SITUATION (COMMUNE_ID);
CREATE INDEX IDX_SIT_RF_IMMEUBLE_ID ON RF_SITUATION (IMMEUBLE_ID);
ALTER TABLE RF_SURFACE_AU_SOL ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_SURF_SOL_RF_SURFACE ON RF_SURFACE_AU_SOL (SURFACE);
CREATE INDEX IDX_SURF_SOL_RF_TYPE ON RF_SURFACE_AU_SOL (TYPE);
CREATE INDEX IDX_SURF_SOL_RF_IMMEUBLE_ID ON RF_SURFACE_AU_SOL (IMMEUBLE_ID);
ALTER TABLE RF_SURFACE_TOTALE ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_SURF_TOT_RF_IMMEUBLE_ID ON RF_SURFACE_TOTALE (IMMEUBLE_ID);

--
-- Rapprochement RF (= lien entre un TiersRF et un Contribuable)
--

CREATE TABLE RAPPROCHEMENT_RF
(
    ID NUMBER(19,0) NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10,0),
    DATE_FIN NUMBER(10,0),
    TYPE_RAPPROCHEMENT NVARCHAR2(15) NOT NULL,
    RF_TIERS_ID NUMBER(19,0) NOT NULL,
    CTB_ID NUMBER(19,0) NOT NULL,
    PRIMARY KEY (ID)
);
ALTER TABLE RAPPROCHEMENT_RF ADD CONSTRAINT FK_RAPPRF_RFTIERS_ID FOREIGN KEY (RF_TIERS_ID) REFERENCES RF_AYANT_DROIT;
ALTER TABLE RAPPROCHEMENT_RF ADD CONSTRAINT FK_RAPPRF_CTB_ID FOREIGN KEY (CTB_ID) REFERENCES TIERS;
CREATE INDEX IDX_RAPPRF_RFTIERS_ID ON RAPPROCHEMENT_RF (RF_TIERS_ID ASC);
CREATE INDEX IDX_RAPPRF_CTB_ID ON RAPPROCHEMENT_RF (CTB_ID ASC);

--
-- Allègements fonciers (= IFONC + dégrèvements ICI)
--

CREATE TABLE ALLEGEMENT_FONCIER (
    ID NUMBER(19,0) NOT NULL,
    TYPE_ALLEGEMENT NVARCHAR2(20) NOT NULL,
    ANNULATION_DATE TIMESTAMP,
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP,
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP,
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10,0) NOT NULL,
    DATE_FIN NUMBER(10,0),
    CTB_ID NUMBER(19,0) NOT NULL,
    IMMEUBLE_ID NUMBER(19,0) NOT NULL,
    DEG_LOC_REVENU NUMBER(10,0),
    DEG_LOC_VOLUME NUMBER(10,0),
    DEG_LOC_SURFACE NUMBER(10,0),
    DEG_LOC_POURCENT NUMBER(5,2),
    DEG_LOC_POURCENT_ARRETE NUMBER(5,2),
    DEG_PRUS_REVENU NUMBER(10,0),
    DEG_PRUS_VOLUME NUMBER(10,0),
    DEG_PRUS_SURFACE NUMBER(10,0),
    DEG_PRUS_POURCENT NUMBER(5,2),
    DEG_PRUS_POURCENT_ARRETE NUMBER(5,2),
    DEG_LL_OCTROI NUMBER(10,0),
    DEG_LL_ECHEANCE NUMBER(10,0),
    DEG_LL_CARAC_SOCIAL_POURCENT NUMBER(5,2),
    IFONC_POURCENT_EXO NUMBER(5,2),
    PRIMARY KEY (ID)
);
ALTER TABLE ALLEGEMENT_FONCIER ADD CONSTRAINT FK_AFONC_CTB_ID FOREIGN KEY (CTB_ID) REFERENCES TIERS (NUMERO);
ALTER TABLE ALLEGEMENT_FONCIER ADD CONSTRAINT FK_AFONC_RF_IMMEUBLE_ID FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_AFONC_CTB_ID ON ALLEGEMENT_FONCIER (CTB_ID);
CREATE INDEX IDX_AFONC_RF_IMMEUBLE_ID ON ALLEGEMENT_FONCIER (IMMEUBLE_ID);

ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD CONSTRAINT FK_DD_RF_IMMEUBLE_ID FOREIGN KEY (DD_IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);

-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.0.0', '6.2.2_7.0.0_upgrade');

--
-- [SIFISC-20372] Import des immeubles du RF
--

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
    ETAT NVARCHAR2(9),
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
    DATE_DEBUT NUMBER(10) NOT NULL,
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
    DATE_DEBUT NUMBER(10) NOT NULL,
    DATE_FIN NUMBER(10),
    DATE_DEBUT_OFFICIELLE NUMBER(10),
    MASTER_ID_RF NVARCHAR2(33) NOT NULL,
    MOTIF_DEBUT_CODE NVARCHAR2(255),
    MOTIF_FIN_CODE NVARCHAR2(255),
    NO_AFFAIRE NVARCHAR2(25),
    ID_RF NVARCHAR2(33),
    IDENTIFIANT_DROIT NVARCHAR2(15),
    PART_PROP_DENOM NUMBER(10),
    PART_PROP_NUM NUMBER(10),
    REGIME_PROPRIETE NVARCHAR2(12) NOT NULL,
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
    DATE_DEBUT NUMBER(10) NOT NULL,
    DATE_FIN NUMBER(10),
    DATE_ESTIMATION NUMBER(10),
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
    DATE_DEBUT NUMBER(10) NOT NULL,
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
    DATE_DEBUT NUMBER(10) NOT NULL,
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
    DATE_DEBUT NUMBER(10) NOT NULL,
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
    DATE_DEBUT NUMBER(10) NOT NULL,
    DATE_FIN NUMBER(10),
    SURFACE NUMBER(10) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE UNIQUE INDEX IDX_AYANTDROIT_ID_RF ON RF_AYANT_DROIT (ID_RF);
CREATE INDEX IDX_BATIMENT_MASTER_ID_RF ON RF_BATIMENT (MASTER_ID_RF);
CREATE UNIQUE INDEX IDS_COMMUNE_NO_OFS ON RF_COMMUNE (NO_OFS);
ALTER TABLE RF_DESCRIPTION_BATIMENT ADD FOREIGN KEY (BATIMENT_ID) REFERENCES RF_BATIMENT (ID);
CREATE INDEX IDX_DESCR_BAT_RF_BATIMENT_ID ON RF_DESCRIPTION_BATIMENT (BATIMENT_ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (AYANT_DROIT_ID) REFERENCES RF_AYANT_DROIT (ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (COMMUNAUTE_ID) REFERENCES RF_AYANT_DROIT (ID);
CREATE INDEX IDX_DROIT_MASTER_ID_RF ON RF_DROIT (MASTER_ID_RF);
CREATE INDEX IDX_DROIT_ID_RF ON RF_DROIT (ID_RF);
CREATE INDEX IDX_DROIT_RF_AYANT_DROIT_ID ON RF_DROIT (AYANT_DROIT_ID);
CREATE INDEX IDX_DROIT_RF_IMMEUBLE_ID ON RF_DROIT (IMMEUBLE_ID);
ALTER TABLE RF_ESTIMATION ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_ESTIM_RF_IMMEUBLE_ID ON RF_ESTIMATION (IMMEUBLE_ID);
CREATE UNIQUE INDEX CS_IMMEUBLE_RF_ID ON RF_IMMEUBLE (ID_RF);
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
-- Nouvelle entité
--

CREATE TABLE ETIQUETTE
(
    ID NUMBER(19,0) NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    CODE NVARCHAR2(50) NOT NULL,
    LIBELLE NVARCHAR2(100) NOT NULL,
    TYPE_TIERS NVARCHAR2(15) NOT NULL,
    COLADM_ID NUMBER(19,0),
    EXPEDITEUR_DOCS NUMBER(1,0) NOT NULL,
    ACTIVE NUMBER(1,0) NOT NULL,
    AUTO_DECES NVARCHAR2(255),
    PRIMARY KEY (ID)
);
ALTER TABLE ETIQUETTE ADD CONSTRAINT FK_ETIQ_CA_ID FOREIGN KEY (COLADM_ID) REFERENCES TIERS;
CREATE INDEX IDX_ETIQ_CA_ID ON ETIQUETTE (COLADM_ID ASC);
CREATE UNIQUE INDEX IDX_ETIQ_CODE ON ETIQUETTE (CODE ASC);

-- La nouvelle entité a le numéro 25
INSERT INTO ETIQUETTE (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, CODE, LIBELLE, TYPE_TIERS, COLADM_ID, EXPEDITEUR_DOCS, ACTIVE, AUTO_DECES)
    SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, 'Installation 17R1', CURRENT_DATE, 'Installation 17R1', 'HERITAGE', 'Héritage', 'PP', CA.NUMERO, 1, 1, 'BD:Decalage{+1D};ED:DecalageAvecCorrection{+2Y/EOY}'
    FROM TIERS CA WHERE CA.NUMERO_CA=25;
INSERT INTO ETIQUETTE (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, CODE, LIBELLE, TYPE_TIERS, COLADM_ID, EXPEDITEUR_DOCS, ACTIVE, AUTO_DECES)
    SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, 'Installation 17R1', CURRENT_DATE, 'Installation 17R1', 'COLLABORATEUR', 'DS Collaborateur', 'PP', CA.NUMERO, 1, 1, NULL
    FROM TIERS CA WHERE CA.NUMERO_CA=25;

-- rattrapage des tâches de l'ancienne collectivités "SUCCESSION_ACI" à passer sur la "nouvelle entité"
UPDATE TACHE SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-20149', CA_ID=(SELECT NUMERO FROM TIERS WHERE NUMERO_CA=25)
WHERE CA_ID IN (SELECT NUMERO FROM TIERS WHERE NUMERO_CA=1344);

CREATE TABLE ETIQUETTE_TIERS
(
    ID NUMBER(19,0) NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10,0) NOT NULL,
    DATE_FIN NUMBER(10,0),
    COMMENTAIRE NVARCHAR2(200),
    ETIQUETTE_ID NUMBER(19,0) NOT NULL,
    TIERS_ID NUMBER(19,0) NOT NULL,
    PRIMARY KEY (ID)
);
ALTER TABLE ETIQUETTE_TIERS ADD CONSTRAINT FK_ETIQTIERS_ETIQ_ID FOREIGN KEY (ETIQUETTE_ID) REFERENCES ETIQUETTE;
ALTER TABLE ETIQUETTE_TIERS ADD CONSTRAINT FK_ETIQTIERS_TIERS_ID FOREIGN KEY (TIERS_ID) REFERENCES TIERS;
CREATE INDEX IDX_ETIQTIERS_ETIQ_ID ON ETIQUETTE_TIERS (ETIQUETTE_ID ASC);
CREATE INDEX IDX_ETIQTIERS_TIERS_ID ON ETIQUETTE_TIERS (TIERS_ID ASC);

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
-- Identification manuelle pour les rapprochements RF
--

CREATE INDEX IDX_EVT_IDENT_CTB_TD_EMETT_BID ON EVENEMENT_IDENTIFICATION_CTB (DEMANDE_TYPE ASC, EMETTEUR_ID ASC, BUSINESS_ID ASC);
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD TYPE_CTB_RECHERCHE NVARCHAR2(30);
UPDATE EVENEMENT_IDENTIFICATION_CTB SET TYPE_CTB_RECHERCHE='PERSONNE_PHYSIQUE';
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB MODIFY TYPE_CTB_RECHERCHE NVARCHAR2(30) NOT NULL;
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB MODIFY (NOM NVARCHAR2(255), PRENOMS NVARCHAR2(250));
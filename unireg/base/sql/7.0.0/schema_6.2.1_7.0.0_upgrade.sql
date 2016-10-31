-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.0.0', '6.2.1_7.0.0_upgrade');

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
    DATE_EVENEMENT NUMBER(10),
    ERROR_MESSAGE CLOB,
    ETAT NVARCHAR2(255),
    FILE_URL NVARCHAR2(255)
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
    ETAT NVARCHAR2(9),
    TYPE_ENTITE NVARCHAR2(11) NOT NULL,
    TYPE_MUTATION NVARCHAR2(12) NOT NULL,
    XML_CONTENT CLOB NOT NULL,
    IMPORT_ID NUMBER(19) NOT NULL
);
CREATE INDEX IDX_EV_RF_IMP_ETAT ON EVENEMENT_RF_IMPORT (ETAT);
ALTER TABLE EVENEMENT_RF_MUTATION ADD FOREIGN KEY (IMPORT_ID) REFERENCES EVENEMENT_RF_IMPORT (ID);
CREATE INDEX IDX_EV_RF_MUT_ETAT ON EVENEMENT_RF_MUTATION (ETAT);
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
    ID_RF NVARCHAR2(255) NOT NULL,
    NO_CTB NUMBER(19),
    NO_RF NUMBER(19),
    RAISON_SOCIALE NVARCHAR2(255),
    DATE_NAISSANCE NUMBER(10),
    NOM_PP NVARCHAR2(255),
    PRENOM_PP NVARCHAR2(255)
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
    DATE_DEBUT NUMBER(10) NOT NULL,
    DATE_FIN NUMBER(10),
    ID_RF NVARCHAR2(255) NOT NULL,
    SURFACE NUMBER(10) NOT NULL,
    TYPE_CODE NVARCHAR2(255) NOT NULL,
    TYPE_DESCRIPTION NVARCHAR2(255)
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
    MOTIF_DEBUT_CODE NVARCHAR2(255) NOT NULL,
    MOTIF_DEBUT_DESCRIPTION NVARCHAR2(255),
    MOTIF_FIN_CODE NVARCHAR2(255) NOT NULL,
    MOTIF_FIN_DESCRIPTION NVARCHAR2(255),
    NO_AFFAIRE NVARCHAR2(255) NOT NULL,
    ID_RF NVARCHAR2(255),
    IDENTIFIANT_DROIT NVARCHAR2(255),
    PART_PROP_DENOM NUMBER(10),
    PART_PROP_NUM NUMBER(10),
    REGIME_PROPRIETE NVARCHAR2(255) NOT NULL,
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
    MONTANT NUMBER(19) NOT NULL,
    REFERENCE NVARCHAR2(255) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_IMMEUBLE
(
    TYPE NVARCHAR2(31) NOT NULL,
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    EGRID NVARCHAR2(14),
    ID_RF NVARCHAR2(255) NOT NULL,
    URL_INTERCAPI NVARCHAR2(2000),
    CFA NUMBER(1),
    QUOTE_PART_DENOM NUMBER(10),
    QUOTE_PART_NUM NUMBER(10)
);
CREATE TABLE RF_IMMEUBLE_BATIMENT
(
    IMMEUBLE_ID NUMBER(19) NOT NULL,
    BATIMENT_ID NUMBER(19) NOT NULL,
    CONSTRAINT SYS_C0015311426 PRIMARY KEY (IMMEUBLE_ID, BATIMENT_ID)
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
    NO_OFS_COMMUNE NUMBER(10) NOT NULL,
    NO_PARCELLE NUMBER(10) NOT NULL,
    NO_RF_COMMUNE NUMBER(10) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_SURFACE
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
    TYPE_CODE NVARCHAR2(255) NOT NULL,
    TYPE_DESCRIPTION NVARCHAR2(255),
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (AYANT_DROIT_ID) REFERENCES RF_AYANT_DROIT (ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (COMMUNAUTE_ID) REFERENCES RF_AYANT_DROIT (ID);
CREATE INDEX IDX_DROIT_RF_AYANT_DROIT_ID ON RF_DROIT (AYANT_DROIT_ID);
CREATE INDEX IDX_DROIT_RF_IMMEUBLE_ID ON RF_DROIT (IMMEUBLE_ID);
ALTER TABLE RF_ESTIMATION ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_ESTIM_RF_IMMEUBLE_ID ON RF_ESTIMATION (IMMEUBLE_ID);
ALTER TABLE RF_IMMEUBLE_BATIMENT ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
ALTER TABLE RF_IMMEUBLE_BATIMENT ADD FOREIGN KEY (BATIMENT_ID) REFERENCES RF_BATIMENT (ID);
ALTER TABLE RF_SITUATION ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_SIT_RF_IMMEUBLE_ID ON RF_SITUATION (IMMEUBLE_ID);
ALTER TABLE RF_SURFACE ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_SURF_RF_IMMEUBLE_ID ON RF_SURFACE (IMMEUBLE_ID);

-- SIFISC-21727 - Interface Organisation: utiliser le nouvel attribut "islatestsnapshot" pour détecter toutes les modifications dans le passé.
ALTER TABLE EVENEMENT_ORGANISATION ADD (CORRECTION_DANS_PASSE NUMBER(1,0));
UPDATE EVENEMENT_ORGANISATION SET CORRECTION_DANS_PASSE=0;
ALTER TABLE EVENEMENT_ORGANISATION MODIFY CORRECTION_DANS_PASSE NUMBER(1) NOT NULL;

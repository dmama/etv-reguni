-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.1.1', '7.1.0_7.1.1_upgrade');

-- [SIFISC-23908] Ajout d'un flag "contrôle office logement" sur les données de dégrèvement ICI
ALTER TABLE ALLEGEMENT_FONCIER ADD DEG_LL_CTRL_OFFICE_LOGEMENT NUMBER(1,0) NULL;
UPDATE ALLEGEMENT_FONCIER SET DEG_LL_CTRL_OFFICE_LOGEMENT=1 WHERE DEG_LL_CARAC_SOCIAL_POURCENT IS NOT NULL OR DEG_LL_ECHEANCE IS NOT NULL OR DEG_LL_OCTROI IS NOT NULL;
UPDATE ALLEGEMENT_FONCIER SET DEG_LL_CTRL_OFFICE_LOGEMENT=0 WHERE TYPE_ALLEGEMENT='DegrevementICI' AND DEG_LL_CTRL_OFFICE_LOGEMENT IS NULL;

-- [SIFISC-23908] Ajout d'un flag "erreur d'intégration" sur les données de dégrèvement ICI
ALTER TABLE ALLEGEMENT_FONCIER ADD DEG_NON_INTEGRABLE NUMBER(1,0) NULL;
UPDATE ALLEGEMENT_FONCIER SET DEG_NON_INTEGRABLE=0 WHERE TYPE_ALLEGEMENT='DegrevementICI';

-- [SIFISC-23894] ajout des raisons d'acquisition
CREATE TABLE RF_RAISON_ACQUISITION
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_ACQUISITION NUMBER(10),
    MOTIF_ACQUISITION NVARCHAR2(255),
    NO_AFFAIRE NVARCHAR2(40),
    DROIT_ID NUMBER(19) NOT NULL,
    CONSTRAINT FK_RAISON_ACQ_RF_IMMEUBLE_ID FOREIGN KEY (DROIT_ID) REFERENCES RF_DROIT (ID)
);
CREATE INDEX IDX_RAISON_ACQ_RF_DROIT_ID ON RF_RAISON_ACQUISITION (DROIT_ID);

-- [SIFISC-23957] re-écriture du modèle de données des servitudes + effacement des données historisées sur les droits
drop table RF_SURFACE_TOTALE cascade constraints;
drop table RF_SURFACE_AU_SOL cascade constraints;
drop table RF_IMPLANTATION cascade constraints;
drop table RF_ESTIMATION cascade constraints;
drop table RF_RAISON_ACQUISITION cascade constraints;
drop table RF_DROIT cascade constraints;
drop table RF_DESCRIPTION_BATIMENT cascade constraints;

drop table EVENEMENT_RF_IMPORT cascade constraints;
drop table EVENEMENT_RF_MUTATION cascade constraints;

-- l'existence d'une situation est obligatoire pour chaque immeuble -> on efface juste l'historique
DELETE FROM RF_SITUATION to_delete  -- efface toutes les situations sauf la première de chaque immeuble
WHERE EXISTS(SELECT NULL
             FROM RF_SITUATION to_keep
             WHERE to_delete.IMMEUBLE_ID = to_keep.IMMEUBLE_ID
                   AND to_delete.ID != to_keep.ID
                   AND (to_keep.DATE_DEBUT IS NULL OR to_keep.DATE_DEBUT < to_delete.DATE_DEBUT));
UPDATE RF_SITUATION set DATE_FIN = null WHERE DATE_FIN is not null;    -- reset la date de fin si nécessaire

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
    ETAT NVARCHAR2(13) NOT NULL,
    FILE_URL NVARCHAR2(512),
    TYPE NVARCHAR2(12) NOT NULL
);
CREATE INDEX IDX_EV_RF_IMP_ETAT ON EVENEMENT_RF_IMPORT (ETAT);
CREATE INDEX IDX_EV_RF_IMP_TYPE ON EVENEMENT_RF_IMPORT (TYPE);
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
    ETAT NVARCHAR2(13),
    ID_RF NVARCHAR2(33),
    TYPE_ENTITE NVARCHAR2(14) NOT NULL,
    TYPE_MUTATION NVARCHAR2(13) NOT NULL,
    XML_CONTENT CLOB,
    IMPORT_ID NUMBER(19) NOT NULL,
    CONSTRAINT FK_EV_MUT_RF_IMPORT_ID FOREIGN KEY (IMPORT_ID) REFERENCES EVENEMENT_RF_IMPORT (ID)
);
CREATE INDEX IDX_EV_RF_MUT_ETAT ON EVENEMENT_RF_MUTATION (ETAT);
CREATE INDEX IDX_EV_RF_ID_RF ON EVENEMENT_RF_MUTATION (ID_RF);
CREATE INDEX IDX_EV_RF_MUT_TYPE_ENTITE ON EVENEMENT_RF_MUTATION (TYPE_ENTITE);
CREATE INDEX IDX_EV_RF_IMP_ID ON EVENEMENT_RF_MUTATION (IMPORT_ID);

-- Tables pour les données du registre foncier
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
    BATIMENT_ID NUMBER(19) NOT NULL,
    CONSTRAINT FK_DESCR_BAT_RF_BATIMENT_ID FOREIGN KEY (BATIMENT_ID) REFERENCES RF_BATIMENT (ID)
);
CREATE INDEX IDX_DESCR_BAT_RF_BATIMENT_ID ON RF_DESCRIPTION_BATIMENT (BATIMENT_ID);

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
    DATE_DEBUT_METIER NUMBER(10),
    DATE_FIN_METIER NUMBER(10),
    MASTER_ID_RF NVARCHAR2(33) NOT NULL,
    MOTIF_DEBUT_CODE NVARCHAR2(255),
    MOTIF_FIN_CODE NVARCHAR2(255),
    IDENTIFIANT_DROIT NVARCHAR2(15),
    NO_AFFAIRE NVARCHAR2(40),
    PART_PROP_DENOM NUMBER(10),
    PART_PROP_NUM NUMBER(10),
    REGIME_PROPRIETE NVARCHAR2(12),
    AYANT_DROIT_ID NUMBER(19),
    IMMEUBLE_ID NUMBER(19),
    COMMUNAUTE_ID NUMBER(19),
    CONSTRAINT FK_DROIT_RF_AYANT_DROIT_ID FOREIGN KEY (AYANT_DROIT_ID) REFERENCES RF_AYANT_DROIT (ID),
    CONSTRAINT FK_DROIT_RF_IMMEUBLE_ID FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID),
    CONSTRAINT FK_DROIT_RF_COMMUNAUTE_ID FOREIGN KEY (COMMUNAUTE_ID) REFERENCES RF_AYANT_DROIT (ID)
);
CREATE UNIQUE INDEX IDX_DROIT_MASTER_ID_RF ON RF_DROIT (MASTER_ID_RF);
CREATE INDEX IDX_DROIT_RF_AYANT_DROIT_ID ON RF_DROIT (AYANT_DROIT_ID);
CREATE INDEX IDX_DROIT_RF_IMMEUBLE_ID ON RF_DROIT (IMMEUBLE_ID);
CREATE INDEX IDX_DROIT_RF_COMM_ID ON RF_DROIT (COMMUNAUTE_ID);

CREATE TABLE RF_SERVITUDE_AYANT_DROIT
(
    DROIT_ID NUMBER(19) NOT NULL,
    AYANT_DROIT_ID NUMBER(19) NOT NULL,
    CONSTRAINT PK_SERV_AD_RF PRIMARY KEY (DROIT_ID, AYANT_DROIT_ID),
    CONSTRAINT FK_SERV_AD_RF_DROIT_ID FOREIGN KEY (DROIT_ID) REFERENCES RF_DROIT (ID),
    CONSTRAINT FK_SERV_AD_RF_AYANT_DROIT_ID FOREIGN KEY (AYANT_DROIT_ID) REFERENCES RF_AYANT_DROIT (ID)
);

CREATE TABLE RF_SERVITUDE_IMMEUBLE
(
    DROIT_ID NUMBER(19) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL,
    CONSTRAINT PK_SERV_IMM_RF PRIMARY KEY (DROIT_ID, IMMEUBLE_ID),
    CONSTRAINT FK_SERV_IMM_RF_DROIT_ID FOREIGN KEY (DROIT_ID) REFERENCES RF_DROIT (ID),
    CONSTRAINT FK_SERV_IMM_RF_IMMEUBLE_ID FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID)
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
    ANNEE_REFERENCE NUMBER(10),
    DATE_DEBUT_METIER NUMBER(10),
    DATE_FIN_METIER NUMBER(10),
    DATE_INSCRIPTION NUMBER(10),
    EN_REVISION NUMBER(1) NOT NULL,
    MONTANT NUMBER(19),
    REFERENCE NVARCHAR2(25),
    IMMEUBLE_ID NUMBER(19) NOT NULL,
    CONSTRAINT FK_ESTIM_RF_IMMEUBLE_ID FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID)
);
CREATE INDEX IDX_ESTIM_RF_IMMEUBLE_ID ON RF_ESTIMATION (IMMEUBLE_ID);

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
    IMMEUBLE_ID NUMBER(19) NOT NULL,
    CONSTRAINT FK_IMPLANTATION_RF_BATIMENT_ID FOREIGN KEY (BATIMENT_ID) REFERENCES RF_BATIMENT (ID),
    CONSTRAINT FK_IMPLANTATION_RF_IMMEUBLE_ID FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID)
);
CREATE INDEX IDX_IMPLANT_RF_BATIMENT_ID ON RF_IMPLANTATION (BATIMENT_ID);
CREATE INDEX IDX_IMPLANT_RF_IMMEUBLE_ID ON RF_IMPLANTATION (IMMEUBLE_ID);

CREATE TABLE RF_RAISON_ACQUISITION
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_ACQUISITION NUMBER(10),
    MOTIF_ACQUISITION NVARCHAR2(255),
    NO_AFFAIRE NVARCHAR2(40),
    DROIT_ID NUMBER(19) NOT NULL,
    CONSTRAINT FK_RAISON_ACQ_RF_DROIT_ID FOREIGN KEY (DROIT_ID) REFERENCES RF_DROIT (ID)
);
CREATE INDEX IDX_RAISON_ACQ_RF_DROIT_ID ON RF_RAISON_ACQUISITION (DROIT_ID);

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
    IMMEUBLE_ID NUMBER(19) NOT NULL,
    CONSTRAINT FK_SURF_SOL_RF_IMMEUBLE_ID FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID)
);
CREATE INDEX IDX_SURF_SOL_RF_SURFACE ON RF_SURFACE_AU_SOL (SURFACE);
CREATE INDEX IDX_SURF_SOL_RF_TYPE ON RF_SURFACE_AU_SOL (TYPE);
CREATE INDEX IDX_SURF_SOL_RF_IMMEUBLE_ID ON RF_SURFACE_AU_SOL (IMMEUBLE_ID);

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
    IMMEUBLE_ID NUMBER(19) NOT NULL,
    CONSTRAINT FK_SURF_TOT_RF_IMMEUBLE_ID FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID)
);
CREATE INDEX IDX_SURF_TOT_RF_IMMEUBLE_ID ON RF_SURFACE_TOTALE (IMMEUBLE_ID);

-- [SIFISC-23895] ajout des droits entre immeubles
ALTER TABLE RF_AYANT_DROIT ADD IMMEUBLE_ID NUMBER(19) NULL;
ALTER TABLE RF_AYANT_DROIT ADD CONSTRAINT FK_IMM_BENE_RF_IMMEUBLE_ID FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE UNIQUE INDEX IDX_AYANTDROIT_IMMEUBLE_ID ON RF_AYANT_DROIT (IMMEUBLE_ID);
ALTER TABLE RF_DROIT MODIFY REGIME_PROPRIETE NVARCHAR2(14);
-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.0.0', '6.2.1_7.0.0_upgrade');

--
-- [SIFISC-20372] Import des immeubles du RF
--

create table EVENEMENT_RF_IMMEUBLE (
	id number(19,0) not null,
	ANNULATION_DATE timestamp,
	ANNULATION_USER nvarchar2(65),
	LOG_CDATE timestamp,
	LOG_CUSER nvarchar2(65),
	LOG_MDATE timestamp,
	LOG_MUSER nvarchar2(65),
	DATE_EVENEMENT number(10,0),
	ETAT nvarchar2(10),
	FILE_URL nvarchar2(255),
	ERROR_MESSAGE blob,
	primary key (id)
);
create index IDX_EV_RF_IMM_ETAT on EVENEMENT_RF_IMMEUBLE (ETAT);

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
    NO_AFFAIRE NVARCHAR2(255),
    ID_RF NVARCHAR2(255),
    IDENTIFIANT_DROIT NVARCHAR2(255),
    PART_PROP_DENOM NUMBER(10),
    PART_PROP_NUM NUMBER(10),
    REGIME_PROPRIETE NVARCHAR2(255) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL,
    TIERS_ID NUMBER(19) NOT NULL
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
    DATE_ESTIMATION RAW(255) NOT NULL,
    EN_REVISION NUMBER(1) NOT NULL,
    MONTANT NUMBER(19) NOT NULL,
    REFERENCE NVARCHAR2(255) NOT NULL,
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_IMMEUBLE
(
    ID NUMBER(19) PRIMARY KEY NOT NULL,
    CFA NUMBER(1) NOT NULL,
    EGID NUMBER(19),
    ID_RF NVARCHAR2(255) NOT NULL,
    QUOTE_PART_DENOM NUMBER(10),
    QUOTE_PART_NUM NUMBER(10),
    TYPE NVARCHAR2(255) NOT NULL,
    URL_INTERCAPI NVARCHAR2(2000)
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
    NO_OFS_COMMUNE NUMBER(10),
    NO_PARCELLE NUMBER(10) NOT NULL,
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
    IMMEUBLE_ID NUMBER(19) NOT NULL
);
CREATE TABLE RF_TIERS
(
    TIERS_TYPE NVARCHAR2(31) NOT NULL,
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
    DATE_NAISSANCE RAW(255),
    NOM_PP NVARCHAR2(255),
    PRENOM_PP NVARCHAR2(255)
);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
ALTER TABLE RF_DROIT ADD FOREIGN KEY (TIERS_ID) REFERENCES RF_TIERS (ID);
CREATE INDEX IDX_DROIT_RF_IMMEUBLE_ID ON RF_DROIT (IMMEUBLE_ID);
CREATE INDEX IDX_DROIT_RF_TIERS_ID ON RF_DROIT (TIERS_ID);
ALTER TABLE RF_ESTIMATION ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_ESTIM_RF_IMMEUBLE_ID ON RF_ESTIMATION (IMMEUBLE_ID);
ALTER TABLE RF_SITUATION ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_SIT_RF_IMMEUBLE_ID ON RF_SITUATION (IMMEUBLE_ID);
ALTER TABLE RF_SURFACE ADD FOREIGN KEY (IMMEUBLE_ID) REFERENCES RF_IMMEUBLE (ID);
CREATE INDEX IDX_SURF_RF_IMMEUBLE_ID ON RF_SURFACE (IMMEUBLE_ID);
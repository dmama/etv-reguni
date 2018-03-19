-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.9.0', '5.8.2_5.9.0_upgrade');

-- [SIFISC-12136] Ajout des champs de saisie des noms/prénoms des parents d'une personne physique non-résidente
ALTER TABLE TIERS ADD (NH_NOM_PERE NVARCHAR2(100), NH_PRENOMS_PERE NVARCHAR2(100), NH_NOM_MERE NVARCHAR2(100), NH_PRENOMS_MERE NVARCHAR2(100));

--[SIFISC-11689] Ajout des identifiants d'entreprise IDE
CREATE TABLE IDENTIFICATION_ENTREPRISE (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), NUMERO_IDE NVARCHAR2(12) NOT NULL, TIERS_ID NUMBER(19,9) NOT NULL, PRIMARY KEY (ID));
CREATE INDEX IDX_ID_ENTREPRISE_TIERS_ID ON IDENTIFICATION_ENTREPRISE(TIERS_ID);
ALTER TABLE IDENTIFICATION_ENTREPRISE ADD CONSTRAINT FK_IDE_TIERS_ID FOREIGN KEY (TIERS_ID) REFERENCES TIERS;

-- [SIFISC-12424] Ajout du champs de saisie pour les prénoms multiples des non-habitants
ALTER TABLE TIERS ADD (NH_TOUS_PRENOMS NVARCHAR2(100));

-- [SIFISC-12571] réception des informations de l'application ReqDes
CREATE TABLE EVENEMENT_REQDES (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	XML CLOB NOT NULL,
	DOUBLON NUMBER(1,0) NOT NULL,
	DATE_ACTE NUMBER(10,0) NOT NULL,
	NUMERO_MINUTE NVARCHAR2(30) NOT NULL,
	VISA_NOTAIRE NVARCHAR2(65) NOT NULL,
	NOM_NOTAIRE NVARCHAR2(100) NOT NULL,
	PRENOM_NOTAIRE NVARCHAR2(100) NOT NULL,
	VISA_OPERATEUR NVARCHAR2(65),
	NOM_OPERATEUR NVARCHAR2(100),
	PRENOM_OPERATEUR NVARCHAR2(100),
	PRIMARY KEY (ID)
);
CREATE INDEX IDX_REQDES_EVT_NO_MINUTE ON EVENEMENT_REQDES(NUMERO_MINUTE ASC, VISA_NOTAIRE ASC);

CREATE TABLE REQDES_UNITE_TRAITEMENT (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	ETAT NVARCHAR2(10) NOT NULL,
	DATE_TRAITEMENT TIMESTAMP,
	EVENEMENT_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE REQDES_UNITE_TRAITEMENT ADD CONSTRAINT FK_REQDES_UT_EVT_ID FOREIGN KEY (EVENEMENT_ID) REFERENCES EVENEMENT_REQDES;
CREATE INDEX IDX_REQDES_UT_EVT_ID ON REQDES_UNITE_TRAITEMENT(EVENEMENT_ID ASC);
CREATE INDEX IDX_REQDES_UT_ETAT ON REQDES_UNITE_TRAITEMENT(ETAT ASC);
CREATE INDEX IDX_REQDES_UT_DT_ETAT ON REQDES_UNITE_TRAITEMENT(DATE_TRAITEMENT ASC, ETAT ASC);

CREATE TABLE REQDES_ERREUR (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	MESSAGE NVARCHAR2(1024),
	CALLSTACK NVARCHAR2(2000),
	TYPE NVARCHAR2(7) NOT NULL,
	UNITE_TRAITEMENT_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE REQDES_ERREUR ADD CONSTRAINT FK_REQDES_UT_ERR_UT_ID FOREIGN KEY (UNITE_TRAITEMENT_ID) REFERENCES REQDES_UNITE_TRAITEMENT;
CREATE INDEX IDX_REQDES_ERREUR_UT_ID ON REQDES_ERREUR(UNITE_TRAITEMENT_ID ASC);

CREATE TABLE REQDES_PARTIE_PRENANTE (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	NOM NVARCHAR2(100) NOT NULL,
	PRENOMS NVARCHAR2(100),
	DATE_NAISSANCE NUMBER(10,0),
	SEXE NVARCHAR2(8),
	DATE_DECES NUMBER(10,0),
	SOURCE_CIVILE NUMBER(1,0) NOT NULL,
	NO_CTB NUMBER(19,0),
	NAVS NVARCHAR2(13),
	NOM_MERE NVARCHAR2(100),
	PRENOMS_MERE NVARCHAR2(100),
	NOM_PERE NVARCHAR2(100),
	PRENOMS_PERE NVARCHAR2(100),
	ETAT_CIVIL NVARCHAR2(34),
	DATE_ETAT_CIVIL NUMBER(10,0),
	DATE_SEPARATION NUMBER(10,0),
	OFS_PAYS_NATIONALITE NUMBER(10,0),
	CATEGORIE_ETRANGER NVARCHAR2(50),
	TEXTE_CASE_POSTALE NVARCHAR2(15),
	NUMERO_CASE_POSTALE NUMBER(10,0),
	LOCALITE NVARCHAR2(100),
	NO_ORDRE_POSTAL NUMBER(10,0),
	NPA NVARCHAR2(35),
	NPA_CPLT NUMBER(10,0),
	OFS_PAYS_RESIDENCE NUMBER(10,0),
	RUE NVARCHAR2(100),
	NUMERO_MAISON NVARCHAR2(35),
	NUMERO_APPARTEMENT NVARCHAR2(35),
	COMPLEMENT_ADRESSE NVARCHAR2(100),
	OFS_COMMUNE_RESIDENCE NUMBER(10,0),
	NOM_CONJOINT NVARCHAR2(100),
	PRENOMS_CONJOINT NVARCHAR2(100),
	CONJOINT_ID NUMBER(19,0),
	UNITE_TRAITEMENT_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE REQDES_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_PP_UT_ID FOREIGN KEY (UNITE_TRAITEMENT_ID) REFERENCES REQDES_UNITE_TRAITEMENT;
ALTER TABLE REQDES_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_PP_CONJOINT_ID FOREIGN KEY (CONJOINT_ID) REFERENCES REQDES_PARTIE_PRENANTE;
CREATE INDEX IDX_REQDES_PP_UT_ID ON REQDES_PARTIE_PRENANTE(UNITE_TRAITEMENT_ID ASC);

CREATE TABLE REQDES_TRANSACTION_IMMOBILIERE (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	OFS_COMMUNE NUMBER(10,0) NOT NULL,
	DESCRIPTION NVARCHAR2(100),
	MODE_INSCRIPTION NVARCHAR2(12) NOT NULL,
	TYPE_INSCRIPTION NVARCHAR2(15) NOT NULL,
	EVENEMENT_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE REQDES_TRANSACTION_IMMOBILIERE ADD CONSTRAINT FK_REQDES_TI_EVT_ID FOREIGN KEY (EVENEMENT_ID) REFERENCES EVENEMENT_REQDES;

CREATE TABLE REQDES_ROLE_PARTIE_PRENANTE (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	ROLE NVARCHAR2(10) NOT NULL,
	PARTIE_PRENANTE_ID NUMBER(19,0) NOT NULL,
	TRANSACTION_IMMOBILIERE_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE REQDES_ROLE_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_RPP_PP_ID FOREIGN KEY (PARTIE_PRENANTE_ID) REFERENCES REQDES_PARTIE_PRENANTE;
ALTER TABLE REQDES_ROLE_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_RPP_TI_ID FOREIGN KEY (TRANSACTION_IMMOBILIERE_ID) REFERENCES REQDES_TRANSACTION_IMMOBILIERE;
CREATE INDEX IDX_REQDES_RPP_PP_ID ON REQDES_ROLE_PARTIE_PRENANTE(PARTIE_PRENANTE_ID ASC);
-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.2', '5.10.1_6.0.2_upgrade');

-- SIFISC-15849 (le numéro d'affaire d'un message ReqDes)
ALTER TABLE EVENEMENT_REQDES ADD NO_AFFAIRE NUMBER(19, 0);

-- Table de la migration PM (inutile à l'application Unireg elle-même, mais utilisée par
-- le programme de migration des PM pour sa reprise en cas de crash)
CREATE TABLE MIGRATION_PM_MAPPING (
	ID NUMBER(19, 0) NOT NULL,
	LOG_DATE TIMESTAMP DEFAULT SYSDATE,
	TYPE_ENTITE NVARCHAR2(20) NOT NULL,
	ID_REGPM NUMBER(19, 0) NOT NULL,
	ID_UNIREG NUMBER(19, 0) NOT NULL,
	PRIMARY KEY (ID)
);
COMMENT ON COLUMN MIGRATION_PM_MAPPING.TYPE_ENTITE IS 'Type de l''entité migrée (entreprise, établissement ou individu).';
COMMENT ON COLUMN MIGRATION_PM_MAPPING.ID_REGPM IS 'Identifiant de l''entité dans Reg-PM (= avant migration).';
COMMENT ON COLUMN MIGRATION_PM_MAPPING.ID_UNIREG IS 'Identifiant de l''entité dans Unireg (= après migration).';
CREATE UNIQUE INDEX IDX_MIGRATION_PM_ENTITE ON MIGRATION_PM_MAPPING(TYPE_ENTITE ASC, ID_REGPM ASC);
CREATE SEQUENCE S_MIGR_PM;

-- Nouvelle séquence pour la numérotation des établissements
CREATE SEQUENCE S_ETB START WITH 3000000 INCREMENT BY 1;

-- Nouvelle colonne dans la table des tiers pour contenir le numéro RCEnt d'une entreprise
ALTER TABLE TIERS ADD NUMERO_ENTREPRISE NUMBER(19, 0);
CREATE INDEX IDX_TIERS_NO_ENTREPRISE ON TIERS(NUMERO_ENTREPRISE ASC);
CREATE INDEX IDX_TIERS_NO_ETABLISSEMENT ON TIERS(NUMERO_ETABLISSEMENT ASC);

-- Nouvelles colonnes dans la table des tiers pour les données propres aux établissements
ALTER TABLE TIERS ADD (ETB_ENSEIGNE NVARCHAR2(250));
ALTER TABLE TIERS ADD (ETB_PRINCIPAL NUMBER(1,0));
ALTER TABLE TIERS ADD (ETB_RAISON_SOCIALE NVARCHAR2(250));

-- Nouvelle table pour les domiciles d'établissement
CREATE TABLE DOMICILE_ETABLISSEMENT (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10,0) NOT NULL,
	DATE_FIN NUMBER(10,0),
	TYPE_AUT_FISC NVARCHAR2(22) NOT NULL,
	NUMERO_OFS_AUT_FISC NUMBER(10,0) NOT NULL,
	ETABLISSEMENT_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE DOMICILE_ETABLISSEMENT ADD CONSTRAINT FK_DOM_ETB_ETB_ID FOREIGN KEY (ETABLISSEMENT_ID) REFERENCES TIERS;
CREATE INDEX IDX_DOM_ETB_ETB_ID ON DOMICILE_ETABLISSEMENT (ETABLISSEMENT_ID ASC);

-- Nouvelle table pour les bouclements
CREATE TABLE BOUCLEMENT (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10, 0) NOT NULL,	-- YYYMMDD
	ANCRAGE NUMBER(4, 0) NOT NULL,			-- MMDD
	PERIODE_MOIS NUMBER(2, 0) NOT NULL,
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	PRIMARY KEY (ID)
);
COMMENT ON COLUMN BOUCLEMENT.DATE_DEBUT IS 'Date de début de validité de la ligne.';
COMMENT ON COLUMN BOUCLEMENT.ANCRAGE IS 'Prochain ancrage de date de bouclement après la date de début de validité.';
COMMENT ON COLUMN BOUCLEMENT.PERIODE_MOIS IS 'Périodicité de bouclement (en mois) depuis le point d''ancrage.';
ALTER TABLE BOUCLEMENT ADD CONSTRAINT FK_BOUCLEMENT_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_BOUCLEMENT_ENTR_ID ON BOUCLEMENT(ENTREPRISE_ID ASC);

-- Nouvelle table pour les régimes fiscaux
CREATE TABLE REGIME_FISCAL (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10, 0) NOT NULL,
	DATE_FIN NUMBER(10, 0),
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	PORTEE NVARCHAR2(2) NOT NULL,
	TYPE NVARCHAR2(20) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE REGIME_FISCAL ADD CONSTRAINT FK_REGFISC_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_REGFISC_ENTR_ID ON REGIME_FISCAL(ENTREPRISE_ID ASC);

-- Nouvelle table pour les données du registre du commerce
CREATE TABLE DONNEES_RC (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10, 0) NOT NULL,
	DATE_FIN NUMBER(10, 0),
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	RAISON_SOCIALE NVARCHAR2(250),
	CAPITAL NUMBER(19, 0),
	MONNAIE_CAPITAL NVARCHAR2(3),
	FORME_JURIDIQUE NVARCHAR2(5),
	PRIMARY KEY (ID)
);
ALTER TABLE DONNEES_RC ADD CONSTRAINT FK_DONRC_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_DONRC_ENTR_ID ON DONNEES_RC(ENTREPRISE_ID ASC);

-- Nouvelle table pour les allègements fiscaux
CREATE TABLE ALLEGEMENT_FISCAL (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10, 0) NOT NULL,
	DATE_FIN NUMBER(10, 0),
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	POURCENTAGE_ALLEGEMENT NUMBER(5, 2) NOT NULL,
	TYPE_IMPOT NVARCHAR2(50),
	TYPE_COLLECTIVITE NVARCHAR2(15),
	NO_OFS_COMMUNE NUMBER(10,0),
	PRIMARY KEY (ID)
);
ALTER TABLE ALLEGEMENT_FISCAL ADD CONSTRAINT FK_ALLFISC_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_ALLFISC_ENTR_ID ON ALLEGEMENT_FISCAL(ENTREPRISE_ID ASC);

-- Nouvelles tables pour les événements organisations
CREATE TABLE EVENEMENT_ORGANISATION (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	IDENT_EMETTEUR NVARCHAR2(5),
	REFDATA_EMETTEUR NVARCHAR2(255),
	COMMENTAIRE_TRAITEMENT NVARCHAR2(255),
	DATE_EVENEMENT NUMBER(10,0) NOT NULL,
	DATE_TRAITEMENT TIMESTAMP,
	ETAT NVARCHAR2(10) NOT NULL,
	NO_ORGANISATION NUMBER(19,0) NOT NULL,
	TYPE NVARCHAR2(40) NOT NULL,
	PRIMARY KEY (ID)
);

CREATE TABLE EVENEMENT_ORGANISATION_ERREUR (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	CALLSTACK NVARCHAR2(2000),
	MESSAGE NVARCHAR2(1024),
	TYPE NVARCHAR2(7) NOT NULL,
	EVT_ORGANISATION_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);

CREATE INDEX IDX_EV_ORGA_ETAT ON EVENEMENT_ORGANISATION (ETAT);
CREATE INDEX IDX_EV_ORGA_NO_ORGA ON EVENEMENT_ORGANISATION (NO_ORGANISATION);

ALTER TABLE EVENEMENT_ORGANISATION_ERREUR ADD CONSTRAINT FK_EV_ERR_EV_ORGA_ID FOREIGN KEY (EVT_ORGANISATION_ID) REFERENCES EVENEMENT_ORGANISATION;

-- Les mandats ont besoin d'un peu plus d'information que les rapports entre tiers classiques
ALTER TABLE RAPPORT_ENTRE_TIERS ADD TYPE_MANDAT NVARCHAR2(15);
ALTER TABLE RAPPORT_ENTRE_TIERS ADD IBAN_MANDAT NVARCHAR2(34);
ALTER TABLE RAPPORT_ENTRE_TIERS ADD BIC_SWIFT_MANDAT NVARCHAR2(15);

-- La séquence S_PM devient S_CAAC (elle gère en fait les Collectivités Administratives et les Autres Communautés...)
RENAME S_PM TO S_CAAC;

-- La nouvelle séquence S_PM gère les entreprises
CREATE SEQUENCE S_PM START WITH 80000 INCREMENT BY 1;

-- Renommage du paramètre "premierePeriodeFiscale" pour distinguer entre le cas des personnes physiques et celui des personnes morales
UPDATE PARAMETRE SET NOM='premierePeriodeFiscalePersonnesPhysiques', LOG_MDATE=CURRENT_DATE, LOG_MUSER='[system-sipm]' WHERE NOM='premierePeriodeFiscale';

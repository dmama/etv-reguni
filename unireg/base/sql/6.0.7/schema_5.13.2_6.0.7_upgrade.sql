-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.7', '5.13.2_6.0.7_upgrade');

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
ALTER TABLE TIERS ADD (ETB_RAISON_SOCIALE NVARCHAR2(250));

-- Ménage sur la table des déclarations, des tiers (colonnes obsolètes)
ALTER TABLE DECLARATION DROP COLUMN NOM_DOCUMENT;
ALTER TABLE TIERS DROP COLUMN REMARQUE;

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
	CODE NVARCHAR2(10) NOT NULL,
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
	FORME_JURIDIQUE NVARCHAR2(15),
	PRIMARY KEY (ID)
);
ALTER TABLE DONNEES_RC ADD CONSTRAINT FK_DONRC_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_DONRC_ENTR_ID ON DONNEES_RC(ENTREPRISE_ID ASC);

-- Le capital, surchargeable, sur une table dédiée
CREATE TABLE CAPITAL_ENTREPRISE (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10,0) NOT NULL,
	DATE_FIN NUMBER(10,0),
	MONTANT NUMBER(19,0) NOT NULL,
	MONNAIE NVARCHAR2(3) NOT NULL,
	ENTREPRISE_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE CAPITAL_ENTREPRISE ADD CONSTRAINT FK_CAP_ENTRP_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_CAP_ENTRP_ID ON CAPITAL_ENTREPRISE (ENTREPRISE_ID ASC, DATE_DEBUT ASC);

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
	TYPE NVARCHAR2(120) NOT NULL,
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
	LIST_INDEX NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);

CREATE INDEX IDX_EV_ORGA_ETAT ON EVENEMENT_ORGANISATION (ETAT);
CREATE INDEX IDX_EV_ORGA_NO_ORGA ON EVENEMENT_ORGANISATION (NO_ORGANISATION);

ALTER TABLE EVENEMENT_ORGANISATION_ERREUR ADD CONSTRAINT FK_EV_ERR_EV_ORGA_ID FOREIGN KEY (EVT_ORGANISATION_ID) REFERENCES EVENEMENT_ORGANISATION;

-- Les mandats ont besoin d'un peu plus d'information que les rapports entre tiers classiques
ALTER TABLE RAPPORT_ENTRE_TIERS ADD TYPE_MANDAT NVARCHAR2(15);
ALTER TABLE RAPPORT_ENTRE_TIERS ADD IBAN_MANDAT NVARCHAR2(34);
ALTER TABLE RAPPORT_ENTRE_TIERS ADD BIC_SWIFT_MANDAT NVARCHAR2(15);

-- le rapport d'activité économique aussi (pour savoir qui est l'établissement principal)
ALTER TABLE RAPPORT_ENTRE_TIERS ADD ETB_PRINCIPAL NUMBER(1,0);

-- La séquence S_PM devient S_CAAC (elle gère en fait les Collectivités Administratives et les Autres Communautés...)
RENAME S_PM TO S_CAAC;

-- La nouvelle séquence S_PM gère les entreprises
CREATE SEQUENCE S_PM START WITH 80000 INCREMENT BY 1;

--
-- Nouveautés dans les paramétrages
-- Renommage de paramètres pour distinguer entre le cas des personnes physiques et celui des personnes morales
--

UPDATE PARAMETRE SET NOM='premierePeriodeFiscalePersonnesPhysiques', LOG_MDATE=CURRENT_DATE, LOG_MUSER='[system-sipm]' WHERE NOM='premierePeriodeFiscale';
UPDATE PARAMETRE SET NOM='delaiEnvoiSommationDeclarationImpotPP', LOG_MDATE=CURRENT_DATE, LOG_MUSER='[system-sipm]' WHERE NOM='delaiEnvoiSommationDeclarationImpot';
UPDATE PARAMETRE SET NOM='delaiEcheanceSommationDeclarationImpotPP', LOG_MDATE=CURRENT_DATE, LOG_MUSER='[system-sipm]' WHERE NOM='delaiEcheanceSommationDeclarationImpot';
UPDATE PARAMETRE SET NOM='delaiRetourDeclarationImpotPPEmiseManuellement', LOG_MDATE=CURRENT_DATE, LOG_MUSER='[system-sipm]' WHERE NOM='delaiRetourDeclarationImpotEmiseManuellement';

--
-- Pour l'instant, pour les tests, on met 2015, mais cela deviendra 2016 pour la production
-- TODO effacer ceci pour le cycle 16R2 au plus tard
--
INSERT INTO PARAMETRE (NOM, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, VALEUR)
		SELECT 'premierePeriodeFiscaleDeclarationPersonnesMorales', CURRENT_DATE, '[system-sipm]', CURRENT_DATE, '[system-sipm]', '2015' FROM DUAL;

--
-- Nouvelle mouture des événements fiscaux
--

-- Mise de côté de l'ancienne table
ALTER TABLE EVENEMENT_FISCAL DROP CONSTRAINT FK_EV_FSC_TRS_ID;
RENAME EVENEMENT_FISCAL TO EVENEMENT_FISCAL_LEGACY;

-- Création de la nouvelle table
CREATE TABLE EVENEMENT_FISCAL (
	ID NUMBER(19,0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	EVT_TYPE NVARCHAR2(29) NOT NULL,
	DATE_VALEUR NUMBER(10,0) NOT NULL,
	TIERS_ID NUMBER(19,0) NOT NULL,
	PARENTE_ENFANT_ID NUMBER(19,0),
	TYPE_EVT_PARENTE NVARCHAR2(25),
	FOR_FISCAL_ID NUMBER(19,0),
	TYPE_EVT_FOR NVARCHAR2(20),
	ALLEGEMENT_ID NUMBER(19,0),
	TYPE_EVT_ALLEGEMENT NVARCHAR2(15),
	TYPE_EVT_INFO_COMPL NVARCHAR2(60),
	DECLARATION_ID NUMBER(19,0),
	TYPE_EVT_DECLARATION NVARCHAR2(15),
	REGIME_FISCAL_ID NUMBER(19,0),
	TYPE_EVT_REGIME NVARCHAR2(15),
	FLAG_ENTREPRISE_ID NUMBER(19,0),
	TYPE_EVT_FLAG NVARCHAR2(15),
	PRIMARY KEY (ID)
);
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_TIERS_ID FOREIGN KEY (TIERS_ID) REFERENCES TIERS;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_ENFANT_ID FOREIGN KEY (PARENTE_ENFANT_ID) REFERENCES TIERS;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_FOR_ID FOREIGN KEY (FOR_FISCAL_ID) REFERENCES FOR_FISCAL;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_ALLGMT_ID FOREIGN KEY (ALLEGEMENT_ID) REFERENCES ALLEGEMENT_FISCAL;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_DECL_ID FOREIGN KEY (DECLARATION_ID) REFERENCES DECLARATION;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_REGFISC_ID FOREIGN KEY (REGIME_FISCAL_ID) REFERENCES REGIME_FISCAL;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_FLAG_ID FOREIGN KEY (FLAG_ENTREPRISE_ID) REFERENCES FLAG_ENTREPRISE;

--
-- Distinction entre les tâches d'envoi de DI PP et PM (les tâches déjà existantes sont toujours
-- relatives aux PP)
--
UPDATE TACHE SET TACHE_TYPE='ENVOI_DI_PP' WHERE TACHE_TYPE='ENVOI_DI';

-- Nouvelle colonne dans la table des tâches pour la catégorie d'entreprise
ALTER TABLE TACHE ADD CATEGORIE_ENTREPRISE NVARCHAR2(10);

--
-- Distinction entre les paramètrages PP et PM par période fiscale
--
ALTER TABLE PARAMETRE_PERIODE_FISCALE ADD PPF_TYPE NVARCHAR2(5);
UPDATE PARAMETRE_PERIODE_FISCALE SET PPF_TYPE='PP';
ALTER TABLE PARAMETRE_PERIODE_FISCALE MODIFY PPF_TYPE NVARCHAR2(5) NOT NULL;
ALTER TABLE PARAMETRE_PERIODE_FISCALE ADD PM_DELAI_IMPRIME_MOIS NUMBER(10,0);
ALTER TABLE PARAMETRE_PERIODE_FISCALE ADD PM_TOLERANCE_JOURS NUMBER(10,0);
ALTER TABLE PARAMETRE_PERIODE_FISCALE ADD PM_DELAI_IMPRIME_FIN_MOIS NUMBER(1,0);
ALTER TABLE PARAMETRE_PERIODE_FISCALE ADD PM_TOLERANCE_FIN_MOIS NUMBER(1,0);
INSERT INTO PARAMETRE_PERIODE_FISCALE (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, PPF_TYPE, PERIODE_ID, TYPE_CTB, PM_DELAI_IMPRIME_MOIS, PM_TOLERANCE_JOURS, PM_DELAI_IMPRIME_FIN_MOIS, PM_TOLERANCE_FIN_MOIS)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '[Installation SIPM]', CURRENT_DATE, '[Installation SIPM]', 'PM', PF.ID, 'VAUDOIS_ORDINAIRE', 6, 75, 0, 0 FROM PERIODE_FISCALE PF;
INSERT INTO PARAMETRE_PERIODE_FISCALE (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, PPF_TYPE, PERIODE_ID, TYPE_CTB, PM_DELAI_IMPRIME_MOIS, PM_TOLERANCE_JOURS, PM_DELAI_IMPRIME_FIN_MOIS, PM_TOLERANCE_FIN_MOIS)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '[Installation SIPM]', CURRENT_DATE, '[Installation SIPM]', 'PM', PF.ID, 'HORS_CANTON', 6, 75, 0, 0 FROM PERIODE_FISCALE PF;
INSERT INTO PARAMETRE_PERIODE_FISCALE (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, PPF_TYPE, PERIODE_ID, TYPE_CTB, PM_DELAI_IMPRIME_MOIS, PM_TOLERANCE_JOURS, PM_DELAI_IMPRIME_FIN_MOIS, PM_TOLERANCE_FIN_MOIS)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '[Installation SIPM]', CURRENT_DATE, '[Installation SIPM]', 'PM', PF.ID, 'HORS_SUISSE', 6, 75, 0, 0 FROM PERIODE_FISCALE PF;

--
-- Le champ "faut-il mettre le code de contrôle sur la sommation de DI" existe pour la DI PP seulement pour le moment
-- (s'il existe un jour pour la DI PM, alors il y aura un autre champ car rien n'oblige les cycles à être synchrones)
--
ALTER TABLE PERIODE_FISCALE RENAME COLUMN CODE_CTRL_SOMM_DI TO CODE_CTRL_SOMM_DI_PP;

--
-- Les états d'entreprise
--
CREATE TABLE ETAT_ENTREPRISE (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_OBTENTION NUMBER(10, 0) NOT NULL,
	TYPE_ETAT NVARCHAR2(20) NOT NULL,
	ENTREPRISE_ID NUMBER(19, 0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE ETAT_ENTREPRISE ADD CONSTRAINT FK_ETAENT_ENTR_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_ETAENT_ENTR_ID ON ETAT_ENTREPRISE(ENTREPRISE_ID ASC);

--
-- Modèles de documents pour les DI PM/APM
-- (TODO pour l'instant,on laisse 2015 comme période minimale, mais pour la production, ce sera 2016 !!!)
--

INSERT INTO MODELE_DOCUMENT (ID, LOG_CDATE, LOG_MDATE, LOG_CUSER, LOG_MUSER, TYPE_DOCUMENT, PERIODE_ID)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, CURRENT_DATE, '[Installation SIPM]', '[Installation SIPM]', 'DECLARATION_IMPOT_PM', ID FROM PERIODE_FISCALE WHERE ANNEE >= 2015;
INSERT INTO MODELE_DOCUMENT (ID, LOG_CDATE, LOG_MDATE, LOG_CUSER, LOG_MUSER, TYPE_DOCUMENT, PERIODE_ID)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, CURRENT_DATE, '[Installation SIPM]', '[Installation SIPM]', 'DECLARATION_IMPOT_APM', ID FROM PERIODE_FISCALE WHERE ANNEE >= 2015;

--
-- les attributs temporels spécifiques aux entreprises (utilité publique, société immobilière...)
--

CREATE TABLE FLAG_ENTREPRISE (
	ID NUMBER(19, 0) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10,0) NOT NULL,
	DATE_FIN NUMBER(10,0),
	FLAG NVARCHAR2(31) NOT NULL,
	ENTREPRISE_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE FLAG_ENTREPRISE ADD CONSTRAINT FK_FLAG_ENTRP_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_FLAG_ENTRP_ID ON FLAG_ENTREPRISE(ENTREPRISE_ID ASC, DATE_DEBUT ASC);

--
-- Ajout de la modélisation nécessaire à la gestion des demandes de délai (en particulier "refusables")
--

ALTER TABLE DELAI_DECLARATION ADD (ETAT NVARCHAR2(10), SURSIS NUMBER(1,0));
UPDATE DELAI_DECLARATION SET ETAT='ACCORDE', SURSIS=0;
ALTER TABLE DELAI_DECLARATION MODIFY (ETAT NVARCHAR2(10) NOT NULL, SURSIS NUMBER(1,0) NOT NULL);
ALTER TABLE DELAI_DECLARATION ADD CLE_ARCHIVAGE_COURRIER NVARCHAR2(40);
UPDATE DELAI_DECLARATION SET CLE_ARCHIVAGE_COURRIER=LPAD(MOD(ID,1000000),6,'0') || ' ' || RPAD('Confirmation Delai', 19, ' ') || ' ' || TO_CHAR(LOG_CDATE, 'MMDDHH24MISSFF3') WHERE CONFIRMATION_ECRITE=1;
ALTER TABLE DELAI_DECLARATION DROP COLUMN CONFIRMATION_ECRITE;

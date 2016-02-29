-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.8', '6.0.7_6.0.8_upgrade');

-- le rapport d'administration d'entreprise a besoin d'un attribut supplémentaire (rôle de 'président')
ALTER TABLE RAPPORT_ENTRE_TIERS ADD ADMIN_PRESIDENT NUMBER(1,0);

-- Changement dans les mappings des types d'allègement
UPDATE ALLEGEMENT_FISCAL SET TYPE_ALLEGEMENT_ICC='EXONERATION_90LI' WHERE TYPE_ALLEGEMENT_ICC IN ('ARTICLE_90IG_LI', 'ARTICLE_90H_LI', 'ARTICLE_90C_LI', 'ARTICLE_90E_LI', 'ARTICLE_90F_LI');
UPDATE ALLEGEMENT_FISCAL SET TYPE_ALLEGEMENT_ICC='TEMPORAIRE_91LI' WHERE TYPE_ALLEGEMENT_ICC IN ('ARTICLE_91_LI');
UPDATE ALLEGEMENT_FISCAL SET TYPE_ALLEGEMENT_ICC='EXONERATION_SPECIALE' WHERE TYPE_ALLEGEMENT_ICC IN ('SOCIETE_MEXICAINE');
UPDATE ALLEGEMENT_FISCAL SET TYPE_ALLEGEMENT_ICC='SOCIETE_SERVICE' WHERE TYPE_ALLEGEMENT_ICC IN ('SOCIETE_DE_BASE');
UPDATE ALLEGEMENT_FISCAL SET TYPE_ALLEGEMENT_ICC='IMMEUBLE_SI_SUBVENTIONNEE' WHERE TYPE_ALLEGEMENT_ICC IN ('IMMEUBLE_SOC_IMMOB_SUBV');
UPDATE ALLEGEMENT_FISCAL SET TYPE_ALLEGEMENT_IFD='TEMPORAIRE_91LI' WHERE TYPE_ALLEGEMENT_IFD IN ('DECISION_DFE');
UPDATE ALLEGEMENT_FISCAL SET TYPE_ALLEGEMENT_IFD='EXONERATION_SPECIALE' WHERE TYPE_ALLEGEMENT_IFD IN ('AUTRE_TYPE');

-- [SIFISC-17979] nouveaux champs sur les mandats
ALTER TABLE RAPPORT_ENTRE_TIERS ADD (NOM_CONTACT_MANDAT NVARCHAR2(50), PRENOM_CONTACT_MANDAT NVARCHAR2(50), TEL_CONTACT_MANDAT NVARCHAR2(35));

-- Ménage sur la table des événements organisation (colonnes obsolètes)
ALTER TABLE EVENEMENT_ORGANISATION DROP COLUMN IDENT_EMETTEUR;
ALTER TABLE EVENEMENT_ORGANISATION DROP COLUMN REFDATA_EMETTEUR;

-- Nouvelle arborescence de classes autour des "autres documents fiscaux"
CREATE TABLE AUTRE_DOCUMENT_FISCAL(
	ID NUMBER(19,0) NOT NULL,
	DOC_TYPE NVARCHAR2(25) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_ENVOI NUMBER(10,0) NOT NULL,
	DELAI_RETOUR NUMBER(10,0),
	DATE_RETOUR NUMBER(10,0),
	DATE_RAPPEL NUMBER(10,0),
	LB_TYPE NVARCHAR2(20),
	ENTREPRISE_ID NUMBER(19,0) NOT NULL,
	PRIMARY KEY (ID)
);
ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD CONSTRAINT FK_ADF_ENT_ID FOREIGN KEY (ENTREPRISE_ID) REFERENCES TIERS;
CREATE INDEX IDX_ADF_ENT_ID ON AUTRE_DOCUMENT_FISCAL (ENTREPRISE_ID ASC);

-- Nouvelle table spécifique aux adresses de mandataires
CREATE TABLE ADRESSE_MANDATAIRE(
	ID NUMBER(19,0) NOT NULL,
	ADR_TYPE NVARCHAR2(31) NOT NULL,
	ANNULATION_DATE TIMESTAMP,
	ANNULATION_USER NVARCHAR2(65),
	LOG_CDATE TIMESTAMP,
	LOG_CUSER NVARCHAR2(65),
	LOG_MDATE TIMESTAMP,
	LOG_MUSER NVARCHAR2(65),
	DATE_DEBUT NUMBER(10,0) NOT NULL,
	DATE_FIN NUMBER(10,0),
	TYPE_MANDAT NVARCHAR2(15) NOT NULL,
	CTB_ID NUMBER(19,0) NOT NULL,
	NOM_DESTINATAIRE NVARCHAR2(100),
	COMPLEMENT NVARCHAR2(100),
	RUE NVARCHAR2(100),
	NUMERO_MAISON NVARCHAR2(35),
	TEXTE_CASE_POSTALE NVARCHAR2(15),
	NUMERO_CASE_POSTALE NUMBER(10,0),
	COMPLEMENT_LOCALITE NVARCHAR2(100),
	NUMERO_POSTAL_LOCALITE NVARCHAR2(100),
	NUMERO_OFS_PAYS NUMBER(10,0),
	NUMERO_RUE NUMBER(10,0),
	NUMERO_ORDRE_POSTE NUMBER(10,0),
	NPA_CASE_POSTALE NUMBER(10,0),
	PRIMARY KEY (ID)
);
ALTER TABLE ADRESSE_MANDATAIRE ADD CONSTRAINT FK_ADR_MAND_CTB_ID FOREIGN KEY (CTB_ID) REFERENCES TIERS;
CREATE INDEX IDX_ADR_MAND_CTB_ID ON ADRESSE_MANDATAIRE (CTB_ID ASC);

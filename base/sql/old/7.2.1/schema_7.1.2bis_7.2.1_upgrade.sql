-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.2.1', '7.1.2bis_7.2.1_upgrade');

-- Cleanup de colonne autour de l'origine des non-habitants plus utilisée depuis la 14R4
ALTER TABLE TIERS DROP COLUMN NH_LIBELLE_COMMUNE_ORIGINE;

--
-- SIFISC-24608 : rassemblement des informations de la personne de contact d'un mandataire sous un seul champ
--

ALTER TABLE RAPPORT_ENTRE_TIERS ADD PERSONNE_CONTACT_MANDAT NVARCHAR2(100) NULL;

UPDATE RAPPORT_ENTRE_TIERS SET PERSONNE_CONTACT_MANDAT=NOM_CONTACT_MANDAT
WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat'
			AND NOM_CONTACT_MANDAT IS NOT NULL
			AND PRENOM_CONTACT_MANDAT IS NOT NULL
			AND REGEXP_LIKE(NOM_CONTACT_MANDAT, '(^|\s|\W)' || REGEXP_REPLACE(PRENOM_CONTACT_MANDAT, '([]\^.$|()[*+?{},])', '\\\1') || '($|\s|\W)', 'i');
UPDATE RAPPORT_ENTRE_TIERS SET PERSONNE_CONTACT_MANDAT=
CASE WHEN PRENOM_CONTACT_MANDAT IS NOT NULL AND NOM_CONTACT_MANDAT IS NOT NULL THEN TRIM(PRENOM_CONTACT_MANDAT || ' ' || NOM_CONTACT_MANDAT)
		 WHEN PRENOM_CONTACT_MANDAT IS NULL THEN NOM_CONTACT_MANDAT
		 WHEN NOM_CONTACT_MANDAT IS NULL THEN PRENOM_CONTACT_MANDAT
END
WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND PERSONNE_CONTACT_MANDAT IS NULL AND (PRENOM_CONTACT_MANDAT IS NOT NULL OR NOM_CONTACT_MANDAT IS NOT NULL);

ALTER TABLE RAPPORT_ENTRE_TIERS DROP COLUMN NOM_CONTACT_MANDAT;
ALTER TABLE RAPPORT_ENTRE_TIERS DROP COLUMN PRENOM_CONTACT_MANDAT;

ALTER TABLE ADRESSE_MANDATAIRE ADD PERSONNE_CONTACT NVARCHAR2(100) NULL;

UPDATE ADRESSE_MANDATAIRE SET PERSONNE_CONTACT=NOM_CONTACT
WHERE NOM_CONTACT IS NOT NULL
			AND PRENOM_CONTACT IS NOT NULL
			AND REGEXP_LIKE(NOM_CONTACT, '(^|\s|\W)' || REGEXP_REPLACE(PRENOM_CONTACT, '([]\^.$|()[*+?{},])', '\\\1') || '($|\s|\W)', 'i');
UPDATE ADRESSE_MANDATAIRE SET PERSONNE_CONTACT=
CASE WHEN PRENOM_CONTACT IS NOT NULL AND NOM_CONTACT IS NOT NULL THEN TRIM(PRENOM_CONTACT || ' ' || NOM_CONTACT)
		 WHEN PRENOM_CONTACT IS NULL THEN NOM_CONTACT
		 WHEN NOM_CONTACT IS NULL THEN PRENOM_CONTACT
END
WHERE PERSONNE_CONTACT IS NULL AND (PRENOM_CONTACT IS NOT NULL OR NOM_CONTACT IS NOT NULL);

ALTER TABLE ADRESSE_MANDATAIRE DROP COLUMN NOM_CONTACT;
ALTER TABLE ADRESSE_MANDATAIRE DROP COLUMN PRENOM_CONTACT;

--
-- SIFISC-24387 ajout de la notion de "civilité" dans les adresses mandataires
--

ALTER TABLE ADRESSE_MANDATAIRE ADD CIVILITE NVARCHAR2(30) NULL;

--
-- SIFISC-24387 recopie des mandats généraux ouverts sur les entreprises en mandats IFONC
--

INSERT INTO ADRESSE_MANDATAIRE (ID, ADR_TYPE, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, DATE_FIN, TYPE_MANDAT, CTB_ID, NOM_DESTINATAIRE,
																COMPLEMENT, RUE, NUMERO_MAISON, TEXTE_CASE_POSTALE, NUMERO_CASE_POSTALE, COMPLEMENT_LOCALITE, NUMERO_POSTAL_LOCALITE,
																NUMERO_OFS_PAYS, NUMERO_RUE, NUMERO_ORDRE_POSTE, NPA_CASE_POSTALE, WITH_COPY, GENRE_IMPOT, TEL_CONTACT, PERSONNE_CONTACT, CIVILITE)
SELECT HIBERNATE_SEQUENCE.NEXTVAL, ADR_TYPE, CURRENT_DATE, 'SQL-SIFISC-24387', CURRENT_DATE, 'SQL-SIFISC-24387', DATE_DEBUT, DATE_FIN, 'SPECIAL', CTB_ID, NOM_DESTINATAIRE,
	COMPLEMENT, RUE, NUMERO_MAISON, TEXTE_CASE_POSTALE, NUMERO_CASE_POSTALE, COMPLEMENT_LOCALITE, NUMERO_POSTAL_LOCALITE,
	NUMERO_OFS_PAYS, NUMERO_RUE, NUMERO_ORDRE_POSTE, NPA_CASE_POSTALE, WITH_COPY, 'IFONC', TEL_CONTACT, PERSONNE_CONTACT, CIVILITE
FROM ADRESSE_MANDATAIRE
WHERE DATE_FIN IS NULL AND ANNULATION_DATE IS NULL AND TYPE_MANDAT='GENERAL' AND CTB_ID < 100000;

INSERT INTO RAPPORT_ENTRE_TIERS (RAPPORT_ENTRE_TIERS_TYPE, ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, DATE_FIN, TIERS_SUJET_ID, TIERS_OBJET_ID,
																 TYPE_MANDAT, IBAN_MANDAT, BIC_SWIFT_MANDAT, TEL_CONTACT_MANDAT, WITH_COPY_MANDAT, GENRE_IMPOT_MANDAT, PERSONNE_CONTACT_MANDAT)
SELECT RAPPORT_ENTRE_TIERS_TYPE, HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, 'SQL-SIFISC-24387', CURRENT_DATE, 'SQL-SIFISC-24387', DATE_DEBUT, DATE_FIN, TIERS_SUJET_ID, TIERS_OBJET_ID,
	'SPECIAL', IBAN_MANDAT, BIC_SWIFT_MANDAT, TEL_CONTACT_MANDAT, WITH_COPY_MANDAT, 'IFONC', PERSONNE_CONTACT_MANDAT
FROM RAPPORT_ENTRE_TIERS
WHERE DATE_FIN IS NULL AND ANNULATION_DATE IS NULL AND RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND TYPE_MANDAT='GENERAL' AND TIERS_SUJET_ID < 100000;

--
-- SIFISC-21645 - Ecran des annonces RCent : ajouter la possibilité de filtrer par forme juridique
--
ALTER TABLE EVENEMENT_ORGANISATION ADD FORME_JURIDIQUE NVARCHAR2(15);
CREATE INDEX IDX_EV_ORGA_FORME_JUR ON EVENEMENT_ORGANISATION (FORME_JURIDIQUE ASC);

-- SIFISC-20376 - ajout des événements fiscaux pour le registre foncier
ALTER TABLE EVENEMENT_FISCAL MODIFY DATE_VALEUR NULL;
ALTER TABLE EVENEMENT_FISCAL MODIFY TIERS_ID NULL;
ALTER TABLE EVENEMENT_FISCAL ADD TYPE_EVT_RAPPROCHEMENT NVARCHAR2(10);
ALTER TABLE EVENEMENT_FISCAL ADD TYPE_EVT_BATIMENT NVARCHAR2(24);
ALTER TABLE EVENEMENT_FISCAL ADD TYPE_EVT_DROIT NVARCHAR2(12);
ALTER TABLE EVENEMENT_FISCAL ADD TYPE_EVT_IMMEUBLE NVARCHAR2(39);
ALTER TABLE EVENEMENT_FISCAL ADD TYPE_EVT_IMPLANTATION NVARCHAR2(12);
ALTER TABLE EVENEMENT_FISCAL ADD RAPPROCHEMENT_ID NUMBER(19) constraint FK_EVTFISC_RAPPR_ID references RAPPROCHEMENT_RF;
ALTER TABLE EVENEMENT_FISCAL ADD BATIMENT_ID NUMBER(19) constraint FK_EVTFISC_BATIMENT_ID references RF_BATIMENT;
ALTER TABLE EVENEMENT_FISCAL ADD DROIT_PROP_ID NUMBER(19) constraint FK_EVTFISC_DROIT_PROP_ID references RF_DROIT;
ALTER TABLE EVENEMENT_FISCAL ADD SERVITUDE_ID NUMBER(19) constraint FK_EVTFISC_SERVITUDE_ID references RF_DROIT;
ALTER TABLE EVENEMENT_FISCAL ADD IMMEUBLE_ID NUMBER(19) constraint FK_EVTFISC_IMMEUBLE_ID references RF_IMMEUBLE;
ALTER TABLE EVENEMENT_FISCAL ADD IMPLANTATION_ID NUMBER(19) constraint FK_EVTFISC_IMPLANTATION_ID references RF_IMPLANTATION;

--
-- SIFISC-25180 : alongement de la colonne NOM_DESTINATAIRE de la table ADRESSE_MANDATAIRE
--
ALTER TABLE ADRESSE_MANDATAIRE MODIFY NOM_DESTINATAIRE NVARCHAR2(182);
-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.2.0', '7.1.2_7.2.0_upgrade');

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

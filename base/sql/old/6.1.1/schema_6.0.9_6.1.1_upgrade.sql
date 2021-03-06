-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.1.1', '6.0.9_6.1.1_upgrade');

--
-- Ajout de paramétres périodiques autour des questionnaires SNC
--

ALTER TABLE PARAMETRE_PERIODE_FISCALE ADD (SNC_RAPPEL_IMPRIME NUMBER(10,0), SNC_RAPPEL_EFFECTIF NUMBER(10,0));
INSERT INTO PARAMETRE_PERIODE_FISCALE (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, PPF_TYPE, PERIODE_ID, SNC_RAPPEL_IMPRIME, SNC_RAPPEL_EFFECTIF)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '[Installation SIPM]', CURRENT_DATE, '[Installation SIPM]', 'SNC', PF.ID, (PF.ANNEE + 1) * 10000 + 315, (PF.ANNEE + 1) * 10000 + 831 FROM PERIODE_FISCALE PF;

--
-- Nouveau événements fiscaux
--

UPDATE EVENEMENT_FISCAL SET EVT_TYPE='DECLARATION_SOMMABLE' WHERE EVT_TYPE='DECLARATION';

--
-- [SIFISC-19345] Fusion des motifs de fermeture de for CESSATION_ACTIVITE et FIN_EXPLOITATION en FIN_EXPLOITATION
--

UPDATE FOR_FISCAL SET MOTIF_OUVERTURE='FIN_EXPLOITATION' WHERE MOTIF_OUVERTURE='CESSATION_ACTIVITE';
UPDATE FOR_FISCAL SET MOTIF_FERMETURE='FIN_EXPLOITATION' WHERE MOTIF_FERMETURE='CESSATION_ACTIVITE';

--
-- Attributs "withCopy" et "codeGenreImpot" sur les liens de type mandat
--

ALTER TABLE RAPPORT_ENTRE_TIERS ADD (WITH_COPY_MANDAT NUMBER(1,0), GENRE_IMPOT_MANDAT NVARCHAR2(10));
UPDATE RAPPORT_ENTRE_TIERS SET WITH_COPY_MANDAT=1 WHERE RAPPORT_ENTRE_TIERS_TYPE='Mandat' AND TYPE_MANDAT IN ('GENERAL');
ALTER TABLE ADRESSE_MANDATAIRE ADD (WITH_COPY NUMBER(1,0), GENRE_IMPOT NVARCHAR2(10));
UPDATE ADRESSE_MANDATAIRE SET WITH_COPY=1;
ALTER TABLE ADRESSE_MANDATAIRE MODIFY (WITH_COPY NUMBER(1,0) NOT NULL);

--
-- Nouveau modèle de document pour les questionnaires SNC dès 2016
--

INSERT INTO MODELE_DOCUMENT (ID, LOG_CDATE, LOG_MDATE, LOG_CUSER, LOG_MUSER, TYPE_DOCUMENT, PERIODE_ID)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, CURRENT_DATE, '[16R3]', '[16R3]', 'QUESTIONNAIRE_SNC', ID FROM PERIODE_FISCALE WHERE ANNEE >= 2016;
INSERT INTO MODELE_FEUILLE_DOC (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, MODELE_ID, SORT_INDEX, INTITULE_FEUILLE, NO_CADEV, NO_FORMULAIRE_ACI)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '[Installation SIPM]', CURRENT_DATE, '[Installation SIPM]', MD.ID, 1, 'Questionnaire SNC', 280, 21025
	FROM MODELE_DOCUMENT MD WHERE MD.TYPE_DOCUMENT = 'QUESTIONNAIRE_SNC';
UPDATE DECLARATION D SET D.MODELE_DOC_ID=(SELECT ID FROM MODELE_DOCUMENT M WHERE M.TYPE_DOCUMENT='QUESTIONNAIRE_SNC' AND M.PERIODE_ID=D.PERIODE_ID)
WHERE D.DOCUMENT_TYPE='QSNC' AND D.DATE_FIN >= 20160000;
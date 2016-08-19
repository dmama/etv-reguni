-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.2.0', '6.1.1_6.2.0_upgrade');

--
-- [SIFISC-19984] Emoluments de sommation (50 CHF pour les DI PP dès 2016)
--

ALTER TABLE PARAMETRE_PERIODE_FISCALE ADD (EMOL_TYPE_DOCUMENT NVARCHAR2(15), EMOL_MONTANT NUMBER(8,0));
INSERT INTO PARAMETRE_PERIODE_FISCALE (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, PPF_TYPE, EMOL_TYPE_DOCUMENT, EMOL_MONTANT, PERIODE_ID)
		SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '[Installation 16R4]', CURRENT_DATE, '[Installation 16R4]', 'EMOL', 'SOMMATION_DI_PP', NULL, ID FROM PERIODE_FISCALE
		WHERE ANNEE < 2016;
INSERT INTO PARAMETRE_PERIODE_FISCALE (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, PPF_TYPE, EMOL_TYPE_DOCUMENT, EMOL_MONTANT, PERIODE_ID)
		SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '[Installation 16R4]', CURRENT_DATE, '[Installation 16R4]', 'EMOL', 'SOMMATION_DI_PP', 50, ID FROM PERIODE_FISCALE
		WHERE ANNEE >= 2016;

ALTER TABLE ETAT_DECLARATION ADD EMOLUMENT NUMBER(8,0);

--
-- [SIFISC-19535] Ajout d'une colonne COMMENTAIRE sur les tâches
--

ALTER TABLE TACHE ADD COMMENTAIRE NVARCHAR2(100);

--
-- [SIFISC-20041] Rattrapage des types de document sur les questionnaires SNC migrés pour lesquels Unireg peut devoir être capable de générer des duplicata
--

UPDATE DECLARATION D SET D.MODELE_DOC_ID=(SELECT M.ID FROM MODELE_DOCUMENT M WHERE M.PERIODE_ID=D.PERIODE_ID AND M.TYPE_DOCUMENT='QUESTIONNAIRE_SNC')
WHERE D.MODELE_DOC_ID IS NULL AND D.DOCUMENT_TYPE = 'QSNC';

--
-- [SIFISC-18446] Clés d'archivage des autres documents fiscaux (avec rattrapage pour les documents émis par Unireg après la MeP 16R2)
--

ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD (CLE_ARCHIVAGE NVARCHAR2(40), CLE_ARCHIVAGE_RAPPEL NVARCHAR2(40));
UPDATE AUTRE_DOCUMENT_FISCAL SET CLE_ARCHIVAGE='Lettre bienvenue   ' || TO_CHAR(LOG_CDATE, 'MMDDHH24MISSFF3') WHERE CLE_ARCHIVAGE IS NULL AND LOG_CDATE > TO_DATE(20160612, 'YYYYMMDD') AND DOC_TYPE='LettreBienvenue';
UPDATE AUTRE_DOCUMENT_FISCAL SET CLE_ARCHIVAGE_RAPPEL='Rap_lett_bienvenue ' || TO_CHAR(LOG_CDATE, 'MMDDHH24MISSFF3') WHERE CLE_ARCHIVAGE_RAPPEL IS NULL AND LOG_CDATE > TO_DATE(20160612, 'YYYYMMDD') AND DOC_TYPE='LettreBienvenue' AND DATE_RAPPEL IS NOT NULL;

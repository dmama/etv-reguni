-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.2.2', '6.1.1_6.2.2_upgrade');

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

--
-- [SIFISC-19143] Flag d'utilisation du code de contrôle dans les sommations de DI PM
--

ALTER TABLE PERIODE_FISCALE ADD CODE_CTRL_SOMM_DI_PM NUMBER(1);
UPDATE PERIODE_FISCALE SET CODE_CTRL_SOMM_DI_PM=1 WHERE ANNEE >= 2016;
UPDATE PERIODE_FISCALE SET CODE_CTRL_SOMM_DI_PM=0 WHERE ANNEE < 2016;
ALTER TABLE PERIODE_FISCALE MODIFY CODE_CTRL_SOMM_DI_PM NUMBER(1) NOT NULL;

--
-- [SIFISC-17049] Gestion des mandataires : ajout des coordonnées de contact sur une adresse mandataire
--

ALTER TABLE ADRESSE_MANDATAIRE ADD (NOM_CONTACT NVARCHAR2(50), PRENOM_CONTACT NVARCHAR2(50), TEL_CONTACT NVARCHAR2(35));

--
-- [SIFISC-18446] Autres documents fiscaux : nouveaux champs pour nouveaux documents
-- AR_DATE_DEMANDE : date de la demande de radiation du RC à placer dans le courrier d'autorisation de radiation
-- DBF_PERIODE_FISCALE : période fiscale indiquée dans la lettre de demande de bilan final
-- DBF_DATE_REQ_RADIATION : date de la réquisition de radiation émise par le RC dans la lettre de demande de bilan final
--

ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD AR_DATE_DEMANDE NUMBER(10,0);
ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD (DBF_PERIODE_FISCALE NUMBER(10,0), DBF_DATE_REQ_RADIATION NUMBER(10,0));

--
-- Ajout de la notion de "feuille principale" dans les modèles de feuilles de documents
--

ALTER TABLE MODELE_FEUILLE_DOC ADD PRINCIPAL NUMBER(1);
UPDATE MODELE_FEUILLE_DOC SET PRINCIPAL=1 WHERE NO_CADEV IN (200, 210, 250, 270, 130, 140, 280);
UPDATE MODELE_FEUILLE_DOC SET PRINCIPAL=0 WHERE PRINCIPAL IS NULL;
ALTER TABLE MODELE_FEUILLE_DOC MODIFY PRINCIPAL NUMBER(1) NOT NULL;

-- [SIFISC-19660] Annonce à destination de l'IDE
--

alter table TIERS add (SECTEUR_ACTIVITE nvarchar2(1024), IDE_DIRTY NUMBER(1,0), IDE_DESACTIVE NUMBER(1,0));
update TIERS set IDE_DIRTY=0 where TIERS_TYPE = 'Entreprise';
update TIERS set IDE_DESACTIVE=0 where TIERS_TYPE = 'Entreprise';

alter table EVENEMENT_ORGANISATION add (NO_ANNONCE_IDE number(19,0));

create table REFERENCE_ANNONCE_IDE (
	id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65),
	ETABLISSEMENT_ID number(19,0) NOT NULL,
	MSG_BUSINESS_ID nvarchar2(64),
	primary key (id));

alter table REFERENCE_ANNONCE_IDE add constraint FK_REFANNIDE_ETAB_ID foreign key (ETABLISSEMENT_ID) references TIERS;

create index IDX_EVTANNIDE_ETAB_ID on REFERENCE_ANNONCE_IDE (ETABLISSEMENT_ID);
create index IDX_EVTANNIDE_BUSINESS_ID on REFERENCE_ANNONCE_IDE (MSG_BUSINESS_ID);

alter table EVENEMENT_ORGANISATION add constraint FK_EV_ORG_REFANNIDE_ID foreign key (NO_ANNONCE_IDE) references REFERENCE_ANNONCE_IDE;

-- SIFISC-21727 - Interface Organisation: utiliser le nouvel attribut "islatestsnapshot" pour détecter toutes les modifications dans le passé.
ALTER TABLE EVENEMENT_ORGANISATION ADD (CORRECTION_DANS_PASSE NUMBER(1,0));
UPDATE EVENEMENT_ORGANISATION SET CORRECTION_DANS_PASSE=0;
ALTER TABLE EVENEMENT_ORGANISATION MODIFY CORRECTION_DANS_PASSE NUMBER(1) NOT NULL;

-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.2.1', '6.2.0_6.2.1_upgrade');

--
-- Autres documents fiscaux : nouveaux champs pour nouveaux documents
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

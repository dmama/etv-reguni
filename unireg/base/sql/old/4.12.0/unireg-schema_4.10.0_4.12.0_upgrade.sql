-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.12.0', '4.10.0_4.12.0_upgrade');

--
-- [SIFISC-1796] URL du document support de la demande d'identification de contribuable
--
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (DOCUMENT_URL nvarchar2(255) NULL);

--
-- ATTENTION : La contruction de l'URL doit encore être finalisée par l'ESB... Ne pas déployer
-- le schéma avec des valeurs bidon !
--
--UPDATE EVENEMENT_IDENTIFICATION_CTB SET DOCUMENT_URL='TODO-' || BUSINESS_ID
--WHERE TYPE_MESSAGE IN ('ssk-3001-000101', 'ssk-3002-000101', 'ssk-3002-000102', 'ssk-3002-000103', 'ssk-3002-000104', 'ssk-2001-000201', 'ssk-3001-000201', 'CommandeUnitaireCHM');

-- [SIFISC-2066] tri des modèles de feuilles de document
ALTER TABLE MODELE_FEUILLE_DOC ADD (SORT_INDEX number(10,0) NULL);

-- [SIFISC-1368] code de contrôle sur les déclarations d'impôt
ALTER TABLE DECLARATION ADD (CODE_CONTROLE nvarchar2(6) NULL);

-- [SIFISC-2100] code de segmentation sur les déclarations d'impôt
ALTER TABLE DECLARATION ADD (CODE_SEGMENT number(10,0) NULL);
ALTER TABLE TACHE ADD (CODE_SEGMENT number(10,0) NULL);

-- [SIFISC-1782] source de quittancement des DIs
ALTER TABLE ETAT_DECLARATION ADD (SOURCE nvarchar2(255) NULL);

-- [SIFISC-2337] affichage des immeubles du registre foncier
create table IMMEUBLE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0), DATE_MUTATION number(10,0), ESTIMATION_FISCALE number(10,0), GENRE_PROPRIETE nvarchar2(12) not null, NATURE_IMMEUBLE nvarchar2(11) not null, NO_LOT number(10,0) not null, NO_OFS_COMMUNE number(10,0) not null, NO_PARCELLE number(10,0) not null, NO_SOUSLOT number(10,0) not null, PART_PROPRIETE_DENOMINATEUR number(10,0) not null, PART_PROPRIETE_NUMERATEUR number(10,0) not null, CTB_ID number(19,0) not null, primary key (id));
create index IDX_IMM_CTB_ID on IMMEUBLE (CTB_ID);
alter table IMMEUBLE add constraint FK_IMM_CTB_ID foreign key (CTB_ID) references TIERS;

--[SIFISC-123]Transmetteur et montant pour le NCS
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (MONTANT number(10,0) NULL);
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (TRANSMETTEUR nvarchar2(255) NULL);

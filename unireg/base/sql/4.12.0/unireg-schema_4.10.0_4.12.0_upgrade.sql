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

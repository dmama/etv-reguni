-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.1.0', '7.0.3_7.1.0_upgrade');

-- SIFISC-22396 - import des servitudes
ALTER TABLE EVENEMENT_RF_IMPORT ADD TYPE NVARCHAR2(13) NULL;
UPDATE EVENEMENT_RF_IMPORT set TYPE = 'PRINCIPAL' where TYPE is null;
ALTER TABLE EVENEMENT_RF_IMPORT MODIFY TYPE NOT NULL;
ALTER TABLE EVENEMENT_RF_IMPORT MODIFY ETAT NOT NULL;


-- Nouveaux emplacements pour les clés de visualisation des documents dans RepElec
ALTER TABLE AUTRE_DOCUMENT_FISCAL ADD (CLE_DOCUMENT NVARCHAR2(256) NULL, CLE_DOCUMENT_RAPPEL NVARCHAR2(256) NULL);
ALTER TABLE ETAT_DECLARATION ADD CLE_DOCUMENT NVARCHAR2(256) NULL;
ALTER TABLE DELAI_DECLARATION ADD CLE_DOCUMENT NVARCHAR2(256) NULL;
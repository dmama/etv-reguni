-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.8.0', '4.7.0_4.8.0_upgrade');

--
-- Suppression de la colonne NUMERO_PM qui dupliquait les données de la colonne NUMERO. 
--
ALTER TABLE TIERS DROP COLUMN NUMERO_PM;
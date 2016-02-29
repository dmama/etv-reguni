-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.3.1', '5.3.0_5.3.1_upgrade');

-- [SIFISC-7168] On oublie la sauvegarde de l'adresse mail e-facture
ALTER TABLE TIERS DROP COLUMN ADRESSE_EMAIL_EFACTURE;

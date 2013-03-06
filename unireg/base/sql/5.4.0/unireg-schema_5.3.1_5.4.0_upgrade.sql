-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.4.0', '5.3.1_5.4.0_upgrade');

-- [SIFISC-8177] Marque d'exécution du batch des sourciers rentiers au rôle
ALTER TABLE TIERS ADD RENTIER_SRC_ROLE NUMBER(1,0);

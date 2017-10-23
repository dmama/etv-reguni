-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('18R1.A', '7.3.1_18R1.A_upgrade');

-- [SIFISC-26536] Suppression de la table IMMEUBLE (données de l'extraction Michot)
DROP TABLE IMMEUBLE;
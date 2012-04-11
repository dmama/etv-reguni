-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.0.1', '5.0.0_5.0.1_upgrade');

-- SIFISC-4526: Les requêtes SQL pour obtenir la liste des utilisateurs, des priorités et des types de messages prennent >2 secondes
create index IDX_EVT_IDENT_CTB_TRAIT_USER on EVENEMENT_IDENTIFICATION_CTB (TRAITEMENT_USER);
create index IDX_EVT_IDENT_CTB_ETAT on EVENEMENT_IDENTIFICATION_CTB (ETAT);

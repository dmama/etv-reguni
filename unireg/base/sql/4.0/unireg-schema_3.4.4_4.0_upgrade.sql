-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.0', '3.4.4_4.0_upgrade');

-- Suppression des tables Lucene jamais utilisées
drop table LUCENE_IDX cascade constraints;
drop table LUCENE_IDX_HOST cascade constraints;

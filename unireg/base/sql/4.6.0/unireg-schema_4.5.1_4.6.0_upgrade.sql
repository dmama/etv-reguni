-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.6.0', '4.5.1_4.6.0_upgrade');
--
-- [UNIREG-2245] Identification CTB pour le service NCS
--
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (DEMANDE_TYPE nvarchar2(30));

UPDATE EVENEMENT_IDENTIFICATION_CTB SET DEMANDE_TYPE='MELDEWESEN';
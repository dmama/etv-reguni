-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.9.0', '4.8.0_4.9.0_upgrade');

--
-- [UNIREG-3125] Identification CTB :
--
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (TRAITEMENT_USER nvarchar2(65));

ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (DATE_TRAITEMENT timestamp);

UPDATE EVENEMENT_IDENTIFICATION_CTB SET DATE_TRAITEMENT = LOG_MDATE, TRAITEMENT_USER = LOG_MUSER
WHERE ETAT IN ('TRAITE_MANUELLEMENT','TRAITE_AUTOMATIQUEMENT','TRAITE_MAN_EXPERT','NON_IDENTIFIE');
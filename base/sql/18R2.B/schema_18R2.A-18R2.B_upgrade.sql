-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('18R2.B', '18R2.A_18R2.B_upgrade');

-- [IMM-1139] passage de 33 à 37 caractères la longueur des colonnes contenant des IDs RF.
ALTER TABLE EVENEMENT_RF_MUTATION MODIFY ID_RF NVARCHAR2(37);
ALTER TABLE EVENEMENT_RF_MUTATION MODIFY VERSION_RF NVARCHAR2(37);
ALTER TABLE RF_AYANT_DROIT MODIFY ID_RF NVARCHAR2(37);
ALTER TABLE RF_BATIMENT MODIFY MASTER_ID_RF NVARCHAR2(37);
ALTER TABLE RF_DROIT MODIFY MASTER_ID_RF NVARCHAR2(37);
ALTER TABLE RF_DROIT MODIFY VERSION_ID_RF NVARCHAR2(37);
ALTER TABLE RF_IMMEUBLE MODIFY ID_RF NVARCHAR2(37);

-- [SIFISC-28193] On corrige la date d'obtention pour quelle corresponde réellement à la date de création de l'état (elle contenait en fait la date d'envoir du courrier)
UPDATE ETAT_DOCUMENT_FISCAL
SET DATE_OBTENTION = TO_NUMBER(TO_CHAR(LOG_CDATE, 'YYYYMMDD')), LOG_MDATE = CURRENT_DATE, LOG_MUSER = 'SQL-SIFISC-28193'
WHERE LOG_MUSER = 'SIFISC-28193';

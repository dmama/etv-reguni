-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('18R2.A', '18R1.A_18R2.A_upgrade');

-- [SIFISC-27264] Limitation à 12 caractères des numéros de maison
ALTER TABLE ADRESSE_TIERS MODIFY NUMERO_MAISON NVARCHAR2(12) DEFAULT NULL;
ALTER TABLE ADRESSE_MANDATAIRE MODIFY NUMERO_MAISON NVARCHAR2(12) DEFAULT NULL;
ALTER TABLE REQDES_PARTIE_PRENANTE MODIFY NUMERO_MAISON NVARCHAR2(12) DEFAULT NULL;

-- [SIFISC-28193] Renseignement de la date d'envoi du courrier sur les états 'rappelé' des autres documents fiscaux
UPDATE ETAT_DOCUMENT_FISCAL
SET DATE_ENVOI_COURRIER = DATE_OBTENTION, LOG_MDATE = CURRENT_DATE, LOG_MUSER = 'SIFISC-28193'
WHERE DATE_ENVOI_COURRIER IS NULL AND ETAT_TYPE = 'AUTRE_RAPPELE';

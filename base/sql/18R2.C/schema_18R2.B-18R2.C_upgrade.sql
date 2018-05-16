-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('18R2.C', '18R2.B_18R2.C_upgrade');

-- [SIFISC-28193] rattrapage des 442 documents dont la date d'envoi du courrier est antérieur à la date d'obtention calculée par le script de migration de la 18R2.B
update ETAT_DOCUMENT_FISCAL set DATE_OBTENTION = DATE_ENVOI_COURRIER, LOG_MDATE = CURRENT_DATE, LOG_MUSER = 'SQL-SIFISC-28193' where DATE_ENVOI_COURRIER < DATE_OBTENTION;

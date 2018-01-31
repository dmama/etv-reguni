-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.2.3', '7.2.1_7.2.3_upgrade');

-- SIFISC-24367 : ajouté les surcharges fiscales de communes sur les situations RF.
ALTER TABLE RF_SITUATION ADD NO_OFS_COMMUNE_SURCHARGE NUMBER(10);

-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.0.3', '7.0.2_7.0.3_upgrade');

--
-- [SIFISC-22288] Prise en compte des numéros d'affaire en text libre
--
ALTER TABLE RF_DROIT MODIFY NO_AFFAIRE NVARCHAR2(40) DEFAULT NULL;
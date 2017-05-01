-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.1.2', '7.1.1_7.1.2_upgrade');

-- [SIFISC-24423] ajout des versionsId
ALTER TABLE EVENEMENT_RF_MUTATION ADD VERSION_RF NVARCHAR2(33);

DROP INDEX IDX_DROIT_MASTER_ID_RF;
ALTER TABLE RF_DROIT ADD VERSION_ID_RF NVARCHAR2(33);
CREATE UNIQUE INDEX IDX_DROIT_MASTER_VERSION_ID_RF ON RF_DROIT (MASTER_ID_RF, VERSION_ID_RF);

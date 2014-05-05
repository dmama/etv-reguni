-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.9.0', '5.8.0_5.9.0_upgrade');

-- [SIFISC-12136] Ajout des champs de saisie des noms/prénoms des parents d'une personne physique non-résidente
ALTER TABLE TIERS ADD (NH_NOM_PERE NVARCHAR2(100), NH_PRENOMS_PERE NVARCHAR2(100), NH_NOM_MERE NVARCHAR2(100), NH_PRENOMS_MERE NVARCHAR2(100));

--[SIFISC-11689] Ajout des identifiants d'entreprise IDE
CREATE TABLE IDENTIFICATION_ENTREPRISE (ID NUMBER(19,0) NOT NULL, ANNULATION_DATE TIMESTAMP, ANNULATION_USER NVARCHAR2(65), LOG_CDATE TIMESTAMP, LOG_CUSER NVARCHAR2(65), LOG_MDATE TIMESTAMP, LOG_MUSER NVARCHAR2(65), NUMERO_IDE NVARCHAR2(12) NOT NULL, TIERS_ID NUMBER(19,9) NOT NULL, PRIMARY KEY (ID));
CREATE INDEX IDX_ID_ENTREPRISE_TIERS_ID ON IDENTIFICATION_ENTREPRISE(TIERS_ID);
ALTER TABLE IDENTIFICATION_ENTREPRISE ADD CONSTRAINT FK_IDE_TIERS_ID FOREIGN KEY (TIERS_ID) REFERENCES TIERS;

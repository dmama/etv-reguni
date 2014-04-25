-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.9.0', '5.8.0_5.9.0_upgrade');

-- [SIFISC-12136] Ajout des champs de saisie des noms/prénoms des parents d'une personne physique non-résidente
ALTER TABLE TIERS ADD (NH_NOM_PERE NVARCHAR2(100), NH_PRENOMS_PERE NVARCHAR2(100), NH_NOM_MERE NVARCHAR2(100), NH_PRENOMS_MERE NVARCHAR2(100));
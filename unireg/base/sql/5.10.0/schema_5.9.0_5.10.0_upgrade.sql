-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.10.0', '5.9.0_5.10.0_upgrade');

-- SIFISC-12815
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD COMMENTAIRE_TRAITEMENT nvarchar2(255);

-- SIFISC-13351
ALTER TABLE TIERS ADD NH_LIBELLE_ORIGINE NVARCHAR2(100);
ALTER TABLE TIERS ADD NH_CANTON_ORIGINE NVARCHAR2(2);
ALTER TABLE TIERS ADD NH_NOM_NAISSANCE NVARCHAR2(250);
ALTER TABLE REQDES_PARTIE_PRENANTE ADD NOM_NAISSANCE NVARCHAR2(100);
ALTER TABLE REQDES_PARTIE_PRENANTE ADD LIBELLE_ORIGINE NVARCHAR2(50);
ALTER TABLE REQDES_PARTIE_PRENANTE ADD CANTON_ORIGINE NVARCHAR2(2);
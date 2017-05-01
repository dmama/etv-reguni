-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.2.0', '7.1.2_7.2.0_upgrade');

-- Cleanup de colonne autour de l'origine des non-habitants plus utilisée depuis la 14R4
ALTER TABLE TIERS DROP COLUMN NH_LIBELLE_COMMUNE_ORIGINE;
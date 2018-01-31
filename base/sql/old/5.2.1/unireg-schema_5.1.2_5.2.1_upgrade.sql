-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.2.1', '5.1.2_5.2.1_upgrade');

-- [SIFISC-6511] Mise à null de l'ex-valeur par défaut du champs TEXTE_CASE_POSTALE
update ADRESSE_TIERS set TEXTE_CASE_POSTALE = null where TEXTE_CASE_POSTALE = 'CASE_POSTALE' and NUMERO_CASE_POSTALE is null;
-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.7.0', '4.6.0_4.7.0_upgrade');

--
-- [UNIREG-2923] On remet à null toutes les "autorités tutélaires" qui désignent l'office du tuteur général (collectivité administrative 1013)
--
UPDATE RAPPORT_ENTRE_TIERS SET TIERS_TUTEUR_ID=NULL
WHERE TIERS_TUTEUR_ID IN (SELECT NUMERO FROM TIERS WHERE NUMERO_CA=1013);
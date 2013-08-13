-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.6.0', '5.5.0_5.6.0_upgrade');

-- SIFISC-9096 : flag d'obsolescence des relations de parenté (par exemple pour les enfants dont les parents ne sont pas connus fiscalement au moment de l'arrivée)
ALTER TABLE TIERS ADD PP_DIRTY_PARENTE NUMBER(1,0);
UPDATE TIERS SET PP_DIRTY_PARENTE = 0 WHERE TIERS_TYPE='PersonnePhysique';
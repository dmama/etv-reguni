-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.10.0', '4.9.1_4.10.0_upgrade');

--
-- [UNIREG-2763]  Logiciel Debiteur :
--
ALTER TABLE TIERS ADD (LOGICIEL_ID number(19,0));

--
-- [UNIREG-3244] Evénements fiscaux de naissance et de fin d'autorité parentale
--
ALTER TABLE EVENEMENT_FISCAL ADD (ENFANT_ID number(19,0));

--
-- [UNIREG-3379] Fusion de communes
--
ALTER TABLE EVENEMENT_CIVIL ADD (COMMENTAIRE_TRAITEMENT nvarchar2(255));

-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.10.0', '4.9.0_4.10.0_upgrade');

--
-- [UNIREG-2763]  Logiciel Debiteur :
--
ALTER TABLE TIERS ADD (LOGICIEL_ID number(19,0));

-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.4.0', '4.3.1_4.4.0_upgrade');

--
-- [UNIREG-2412] Ajout de possibilités au service d'identification UniReg asynchrone

--Ajout du Type de mode d'identification
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB add (MODE_IDENTIFICATION nvarchar2(255) default 'MANUEL_SANS_ACK' not null);


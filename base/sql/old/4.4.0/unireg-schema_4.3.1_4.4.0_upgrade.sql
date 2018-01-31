-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.4.0', '4.3.1_4.4.0_upgrade');

--
-- [UNIREG-2412] Ajout de possibilités au service d'identification UniReg asynchrone

--Ajout du mode d'identification
--Ajout du de l'indicateur d'attente de l'identification manuel
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB add (MODE_IDENTIFICATION nvarchar2(255) default 'MANUEL_SANS_ACK' not null,ATTENTE_IDENTIF_MANUEL number(10,0) default 0);

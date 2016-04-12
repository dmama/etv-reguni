-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('3.5.1', '3.4.4_3.5.1_upgrade');

--
-- Retour du numéro de ménage des contribuables identifiées
--
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB add (NO_MENAGE_COMMUN number(19,0));

-- [UNIREG-1850] renseignement de la colonne CA_ID sur toutes les tâches
update TACHE set CA_ID = (select ca.NUMERO from TIERS ctb, TIERS ca where ctb.OID = ca.NUMERO_CA and ctb.NUMERO = TACHE.CTB_ID) where CA_ID is null;


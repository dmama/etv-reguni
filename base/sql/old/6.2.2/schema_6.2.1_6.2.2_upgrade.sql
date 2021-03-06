-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.2.2', '6.2.1_6.2.2_upgrade');

-- SIFISC-21727 - Interface Organisation: utiliser le nouvel attribut "isLatestSnapshot" pour détecter toutes les modifications dans le passé.
ALTER TABLE EVENEMENT_ORGANISATION ADD (CORRECTION_DANS_PASSE NUMBER(1,0));
UPDATE EVENEMENT_ORGANISATION SET CORRECTION_DANS_PASSE=0;
ALTER TABLE EVENEMENT_ORGANISATION MODIFY CORRECTION_DANS_PASSE NUMBER(1) NOT NULL;

-- SIFISC-21902 - Evénements RCEnt: différencier les doublons techniques des événements rejoués dans le passé.
ALTER TABLE EVENEMENT_ORGANISATION ADD (BUSINESS_ID NVARCHAR2(255));

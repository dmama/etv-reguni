-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('3.3.3', '3.3_3.3.3_upgrade');

-- [UNIREG-1341] Correction des données : Transformation des 'ConseilLegal' en 'RepresentationConventionnelle' creer depuis la date de la 1ere mise en prod
update RAPPORT_ENTRE_TIERS set RAPPORT_ENTRE_TIERS_TYPE = 'RepresentationConventionnelle' where LOG_CDATE > TO_DATE('2009-07-13', 'YYYY-MM-DD') AND RAPPORT_ENTRE_TIERS_TYPE = 'ConseilLegal'

--
-- Version 1.3 -> 1.5 XSD
--
alter table EVENEMENT_IDENTIFICATION_CTB modify (BUSINESS_USER varchar2(255));
alter table EVENEMENT_IDENTIFICATION_CTB modify (BUSINESS_ID varchar2(255));
alter table EVENEMENT_IDENTIFICATION_CTB modify (REPLY_TO varchar2(255));
alter table EVENEMENT_IDENTIFICATION_CTB drop column UTILISATEUR_ID;
alter table EVENEMENT_IDENTIFICATION_CTB modify (nom null, prenoms null);
alter table DOC_INDEX modify (DOC_TYPE varchar2(50));
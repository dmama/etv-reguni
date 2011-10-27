-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.3.1', '4.3_4.3.1_upgrade');

--
-- [UNIREG-2399] Fusion des événements civils unitaires et regroupés
--

-- copie des événements civils unitaires en erreur et qui n'existent pas dans les regroupés
insert into EVENEMENT_CIVIL_REGROUPE(ID, ANNULATION_DATE, ANNULATION_USER, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_EVENEMENT, ETAT, NO_INDIVIDU_PRINCIPAL, NUMERO_OFS_ANNONCE, TYPE)
select ecu.ID, ecu.ANNULATION_DATE, ecu.ANNULATION_USER, ecu.LOG_CDATE, ecu.LOG_CUSER, sysdate, '[UNIREG-2399] fusion unitaires-regroupes', ecu.DATE_EVENEMENT, ecu.ETAT, ecu.NUMERO_INDIVIDU, ecu.NUMERO_OFS_ANNONCE, ecu.TYPE
from EVENEMENT_CIVIL_UNITAIRE ecu
where ecu.ETAT != 'TRAITE' and not exists (select ecr.ID from EVENEMENT_CIVIL_REGROUPE ecr where ecr.ID = ecu.ID);

-- suppression du suffixe 'regroupés' de la table des événements civils. La table 'EVENEMENT_CIVIL_UNITAIRE' est gardée pour référence.
alter table EVENEMENT_CIVIL_REGROUPE rename to EVENEMENT_CIVIL;

-- ajout des indexes qui vont bien sur la table evenement civil
create index IDX_EV_CIV_ETAT on EVENEMENT_CIVIL (ETAT);
create index IDX_EV_CIV_NO_IND_PR on EVENEMENT_CIVIL (NO_INDIVIDU_PRINCIPAL);


-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.3.1', '3.5.2_4.3.1_upgrade');

--
-- 3.5.2 -> 4.0
--

-- Suppression des tables Lucene jamais utilisées
drop table LUCENE_IDX cascade constraints;
drop table LUCENE_IDX_HOST cascade constraints;

--
-- 4.0 -> 4.1
--
-- déjà fait en 3.5.2 :
--  o  [UNIREG-1911] Retour du numéro de ménage des contribuables identifiées
--

-- [UNIREG-1140] ajout d'un lien entre un envoi de dossier et l'office émetteur
ALTER TABLE MOUVEMENT_DOSSIER add (OID_EMETTEUR number(19,0));

-- Refactoring de la table des des événements externes
ALTER TABLE EVENEMENT_EXTERNE add (EVENT_TYPE nvarchar2(31) default 'QuittanceLR' not null);
ALTER TABLE EVENEMENT_EXTERNE add (QLR_DATE_DEBUT number(10,0));
ALTER TABLE EVENEMENT_EXTERNE add (QLR_DATE_FIN number(10,0));
ALTER TABLE EVENEMENT_EXTERNE add (QLR_TYPE nvarchar2(13));


--
-- 4.1 -> 4.2
--
-- déjà fait en 3.5.2 :
--  o  Eclatement des types de mouvements de dossiers
--  o  Récupération d'une erreur passée (valeur oubliée dans le script 3.3.3_3.4_upgrade)
--  o  [UNIREG-1850] renseignement de la colonne CA_ID sur toutes les tâches
--  o  ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD WORK_USER nvarchar2(65);
--

alter table EVENEMENT_CIVIL_ERREUR add CALLSTACK nvarchar2(2000);

--
-- 4.2 -> 4.3
--

-- Rattrapage des numéros de tiers dans les événements civils qui ont créé des nouvelles personnes physiques

UPDATE EVENEMENT_CIVIL_REGROUPE ECR
SET HAB_PRINCIPAL = (SELECT NUMERO FROM TIERS T WHERE T.ANNULATION_DATE IS NULL AND T.NUMERO_INDIVIDU = ECR.NO_INDIVIDU_PRINCIPAL)
WHERE ECR.ETAT = 'TRAITE' AND ECR.HAB_PRINCIPAL IS NULL AND ECR.NO_INDIVIDU_PRINCIPAL IS NOT NULL
AND (SELECT COUNT(*) FROM TIERS T WHERE T.ANNULATION_DATE IS NULL AND T.NUMERO_INDIVIDU = ECR.NO_INDIVIDU_PRINCIPAL) = 1;

UPDATE EVENEMENT_CIVIL_REGROUPE ECR
SET HAB_CONJOINT = (SELECT NUMERO FROM TIERS T WHERE T.ANNULATION_DATE IS NULL AND T.NUMERO_INDIVIDU = ECR.NO_INDIVIDU_CONJOINT)
WHERE ECR.ETAT = 'TRAITE' AND ECR.HAB_CONJOINT IS NULL AND ECR.NO_INDIVIDU_CONJOINT IS NOT NULL
AND (SELECT COUNT(*) FROM TIERS T WHERE T.ANNULATION_DATE IS NULL AND T.NUMERO_INDIVIDU = ECR.NO_INDIVIDU_CONJOINT) = 1;

-- [UNIREG-2253] Prépare la réindexation des débiteurs de prestations imposables pour les besoins d'empaci
update TIERS set INDEX_DIRTY = 1 where TIERS_TYPE = 'DebiteurPrestationImposable';

--
-- 4.3 -> 4.3.1
--

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


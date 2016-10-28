-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.2', '3.5.2_4.2_upgrade');

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

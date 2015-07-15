-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.1', '4.0_4.1_upgrade');

-- [UNIREG-1140] ajout d'un lien entre un envoi de dossier et l'office émetteur
ALTER TABLE MOUVEMENT_DOSSIER add (OID_EMETTEUR number(19,0));

-- Refactoring de la table des des événements externes
ALTER TABLE EVENEMENT_EXTERNE add (EVENT_TYPE nvarchar2(31) default 'QuittanceLR' not null);
ALTER TABLE EVENEMENT_EXTERNE add (QLR_DATE_DEBUT number(10,0));
ALTER TABLE EVENEMENT_EXTERNE add (QLR_DATE_FIN number(10,0));
ALTER TABLE EVENEMENT_EXTERNE add (QLR_TYPE nvarchar2(13));

-- [UNIREG-1911] Retour du numéro de ménage des contribuables identifiées
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB add (NO_MENAGE_COMMUN number(19,0));

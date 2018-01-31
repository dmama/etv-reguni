-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.6.0', '5.5.0_5.6.0_upgrade');

-- SIFISC-9096 : flag d'obsolescence des relations de parenté (par exemple pour les enfants dont les parents ne sont pas connus fiscalement au moment de l'arrivée)
ALTER TABLE TIERS ADD PP_PARENTE_DIRTY NUMBER(1,0);
UPDATE TIERS SET PP_PARENTE_DIRTY = 0 WHERE TIERS_TYPE='PersonnePhysique';

-- SIFISC-4455 : numéro AVS fourni par l'UPI lors de l'identification automatique d'un contribuable dont la demande contenait un vieux NAVS13 annulé/remplacé depuis
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD NAVS13_UPI nvarchar2(13);

-- SIFISC-8712 : rattrapage des motifs d'ouverture/fermeture sur les fors débiteurs
UPDATE FOR_FISCAL SET MOTIF_OUVERTURE='INDETERMINE' WHERE MOTIF_OUVERTURE IS NULL AND DATE_OUVERTURE IS NOT NULL AND FOR_TYPE='ForDebiteurPrestationImposable';
UPDATE FOR_FISCAL SET MOTIF_FERMETURE='INDETERMINE' WHERE MOTIF_FERMETURE IS NULL AND DATE_FERMETURE IS NOT NULL AND FOR_TYPE='ForDebiteurPrestationImposable';

-- SIFISC-9561 : tous les flags "sans sommation" doivent être remis à "false"
UPDATE TIERS SET SANS_RAPPEL=0 WHERE SANS_RAPPEL=1 AND TIERS_TYPE='DebiteurPrestationImposable';
UPDATE DECLARATION SET SANS_RAPPEL=0 WHERE SANS_RAPPEL=1 AND DOCUMENT_TYPE='LR';
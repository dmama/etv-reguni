-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.5', '6.0.4_6.0.5_upgrade');

--
-- Déplacement du flag "établissement principal" sur le lien d'activité économique plutôt que sur l'établissement lui-même
-- (car un établissement peut être tour à tour principal et secondaire, en cas de démémagement de siège ou fusion d'entreprise,
-- par exemple...)
--

ALTER TABLE RAPPORT_ENTRE_TIERS ADD ETB_PRINCIPAL NUMBER(1,0);
UPDATE RAPPORT_ENTRE_TIERS RET SET RET.ETB_PRINCIPAL=(SELECT ETB_PRINCIPAL FROM TIERS T WHERE T.NUMERO=RET.TIERS_OBJET_ID AND T.TIERS_TYPE='Etablissement')
WHERE RET.RAPPORT_ENTRE_TIERS_TYPE='ActiviteEconomique';
ALTER TABLE TIERS DROP COLUMN ETB_PRINCIPAL;

--
-- Modèles de documents pour les DI PM/APM
-- (Aujourd'hui, nous mettons 2015 comme année charnière (pour les tests), mais il faudra sans doute mettre 2016 pour la MeP...)
--

INSERT INTO MODELE_DOCUMENT (ID, LOG_CDATE, LOG_MDATE, LOG_CUSER, LOG_MUSER, TYPE_DOCUMENT, PERIODE_ID)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, CURRENT_DATE, '[Installation SIPM]', '[Installation SIPM]', 'DECLARATION_IMPOT_PM', ID FROM PERIODE_FISCALE WHERE ANNEE >= 2015;
INSERT INTO MODELE_DOCUMENT (ID, LOG_CDATE, LOG_MDATE, LOG_CUSER, LOG_MUSER, TYPE_DOCUMENT, PERIODE_ID)
	SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, CURRENT_DATE, '[Installation SIPM]', '[Installation SIPM]', 'DECLARATION_IMPOT_APM', ID FROM PERIODE_FISCALE WHERE ANNEE >= 2015;

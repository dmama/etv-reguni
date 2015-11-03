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


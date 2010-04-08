-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.3', '4.2_4.3_upgrade');

-- Rattrapage des numéros de tiers dans les événements civils qui ont créé des nouvelles personnes physiques

UPDATE EVENEMENT_CIVIL_REGROUPE ECR
SET HAB_PRINCIPAL = (SELECT NUMERO FROM TIERS T WHERE T.ANNULATION_DATE IS NULL AND T.NUMERO_INDIVIDU = ECR.NO_INDIVIDU_PRINCIPAL)
WHERE ECR.ETAT = 'TRAITE' AND ECR.HAB_PRINCIPAL IS NULL AND ECR.NO_INDIVIDU_PRINCIPAL IS NOT NULL
AND (SELECT COUNT(*) FROM TIERS T WHERE T.ANNULATION_DATE IS NULL AND T.NUMERO_INDIVIDU = ECR.NO_INDIVIDU_PRINCIPAL) = 1;

UPDATE EVENEMENT_CIVIL_REGROUPE ECR
SET HAB_CONJOINT = (SELECT NUMERO FROM TIERS T WHERE T.ANNULATION_DATE IS NULL AND T.NUMERO_INDIVIDU = ECR.NO_INDIVIDU_CONJOINT)
WHERE ECR.ETAT = 'TRAITE' AND ECR.HAB_CONJOINT IS NULL AND ECR.NO_INDIVIDU_CONJOINT IS NOT NULL
AND (SELECT COUNT(*) FROM TIERS T WHERE T.ANNULATION_DATE IS NULL AND T.NUMERO_INDIVIDU = ECR.NO_INDIVIDU_CONJOINT) = 1;
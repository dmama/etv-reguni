-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.0.2', '7.0.1_7.0.2_upgrade');

--
-- SIFISC-23194 annulation des tâches assignées à la collectivité 25 (= nouvelle entité) pour des PF < 2015
--
UPDATE TACHE SET ANNULATION_DATE=CURRENT_DATE, ANNULATION_USER='SQL-SIFISC-23194', LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-23194'
WHERE ANNULATION_DATE IS NULL
			AND ETAT='EN_INSTANCE'
			AND TACHE_TYPE = 'ENVOI_DI_PP'
			AND CA_ID = (SELECT NUMERO FROM TIERS WHERE NUMERO_CA=25)
			AND DECL_DATE_FIN < 20150000;

--
-- SIFISC-23112 ajout d'une nouvelle colonne dans les dégrèvements à caractère social pour le pourcentage
--
ALTER TABLE ALLEGEMENT_FONCIER ADD DEG_LL_CARAC_SOCIAL_POURCENT NUMBER(5,2);

--
-- Correction du nom de la colonne pour coller à la dénomination de l'objet métier (il s'agit d'une exonération IFONC, pas d'un allègement IFONC)
--
ALTER TABLE ALLEGEMENT_FONCIER RENAME COLUMN IFONC_POURCENT_ALLGT TO IFONC_POURCENT_EXO;

--
-- [SIFISC-22288] Prise en compte des numéros d'affaire en text libre
--
ALTER TABLE RF_DROIT MODIFY NO_AFFAIRE NVARCHAR2(40) DEFAULT NULL;

--
-- [SIFISC-22995] Calcul des dates de début/fin métier sur les estimations fiscales
--
ALTER TABLE RF_ESTIMATION ADD DATE_DEBUT_METIER NUMBER(10) NULL;
ALTER TABLE RF_ESTIMATION ADD DATE_FIN_METIER NUMBER(10) NULL;
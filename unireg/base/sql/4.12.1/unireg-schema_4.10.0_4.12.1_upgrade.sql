-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('4.12.1', '4.10.0_4.12.1_upgrade');

--
-- [SIFISC-1796] URL du document support de la demande d'identification de contribuable
--
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (DOCUMENT_URL nvarchar2(255) NULL);

--
-- ATTENTION : La contruction de l'URL doit encore être finalisée par l'ESB... Ne pas déployer
-- le schéma avec des valeurs bidon !
--
--UPDATE EVENEMENT_IDENTIFICATION_CTB SET DOCUMENT_URL='TODO-' || BUSINESS_ID
--WHERE TYPE_MESSAGE IN ('ssk-3001-000101', 'ssk-3002-000101', 'ssk-3002-000102', 'ssk-3002-000103', 'ssk-3002-000104', 'ssk-2001-000201', 'ssk-3001-000201', 'CommandeUnitaireCHM');

-- [SIFISC-2066] tri des modèles de feuilles de document
ALTER TABLE MODELE_FEUILLE_DOC ADD (SORT_INDEX number(10,0) NULL);

-- [SIFISC-1368] code de contrôle sur les déclarations d'impôt
ALTER TABLE DECLARATION ADD (CODE_CONTROLE nvarchar2(6) NULL);

-- [SIFISC-2100] code de segmentation sur les déclarations d'impôt
ALTER TABLE DECLARATION ADD (CODE_SEGMENT number(10,0) NULL);
ALTER TABLE TACHE ADD (CODE_SEGMENT number(10,0) NULL);

-- [SIFISC-1782] source de quittancement des DIs
ALTER TABLE ETAT_DECLARATION ADD (SOURCE nvarchar2(255) NULL);

-- [SIFISC-2337] affichage des immeubles du registre foncier
create table IMMEUBLE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0), DATE_MUTATION number(10,0), ESTIMATION_FISCALE number(10,0), GENRE_PROPRIETE nvarchar2(12) not null, NATURE_IMMEUBLE nvarchar2(11) not null, NO_LOT number(10,0) not null, NO_OFS_COMMUNE number(10,0) not null, NO_PARCELLE number(10,0) not null, NO_SOUSLOT number(10,0) not null, PART_PROPRIETE_DENOMINATEUR number(10,0) not null, PART_PROPRIETE_NUMERATEUR number(10,0) not null, CTB_ID number(19,0) not null, primary key (id));
create index IDX_IMM_CTB_ID on IMMEUBLE (CTB_ID);
alter table IMMEUBLE add constraint FK_IMM_CTB_ID foreign key (CTB_ID) references TIERS;

--[SIFISC-123]Transmetteur et montant pour le NCS
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (MONTANT number(10,0) NULL);
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD (TRANSMETTEUR nvarchar2(255) NULL);

--
-- [SIFISC-1933] Ajout de l'identifiant district Fiscal et de l'identifiant Region Fiscal pour un TIERS
--
ALTER TABLE TIERS ADD (DISTRICT_FISCAL_ID number(19,0));
CREATE INDEX IDX_TIERS_CA_DISTRICT ON TIERS (DISTRICT_FISCAL_ID);

ALTER TABLE TIERS ADD (REGION_FISCALE_ID number(19,0));
CREATE INDEX IDX_TIERS_CA_REGION ON TIERS (REGION_FISCALE_ID);

--
-- [SIFISC-1965] Chargement des codes district et région pour les collectivitées administratives.
--
--Aigle
UPDATE TIERS SET DISTRICT_FISCAL_ID = 1, REGION_FISCALE_ID = null WHERE NUMERO_CA = 1;
--Echallens
UPDATE TIERS SET DISTRICT_FISCAL_ID = 2, REGION_FISCALE_ID = null WHERE NUMERO_CA = 5;
--Grandson
UPDATE TIERS SET DISTRICT_FISCAL_ID = 3, REGION_FISCALE_ID = null WHERE NUMERO_CA = 6;
--Lausanne
UPDATE TIERS SET DISTRICT_FISCAL_ID = 4, REGION_FISCALE_ID = 1 WHERE NUMERO_CA = 7;
--La Vallée
UPDATE TIERS SET DISTRICT_FISCAL_ID = 5, REGION_FISCALE_ID = null WHERE NUMERO_CA = 8;
--Lavaux
UPDATE TIERS SET DISTRICT_FISCAL_ID = 6, REGION_FISCALE_ID = null WHERE NUMERO_CA = 9;
--Morges
UPDATE TIERS SET DISTRICT_FISCAL_ID = 7, REGION_FISCALE_ID = null WHERE NUMERO_CA = 10;
--Moudon
UPDATE TIERS SET DISTRICT_FISCAL_ID = 8, REGION_FISCALE_ID = null WHERE NUMERO_CA = 11;
--Nyon
UPDATE TIERS SET DISTRICT_FISCAL_ID = 9, REGION_FISCALE_ID = 2 WHERE NUMERO_CA = 12;
--Orbe
UPDATE TIERS SET DISTRICT_FISCAL_ID = 10, REGION_FISCALE_ID = null WHERE NUMERO_CA = 13;
--Payerne
UPDATE TIERS SET DISTRICT_FISCAL_ID = 11, REGION_FISCALE_ID = null WHERE NUMERO_CA = 15;
--Pays d'Enhaut
UPDATE TIERS SET DISTRICT_FISCAL_ID = 12, REGION_FISCALE_ID = null WHERE NUMERO_CA = 16;
--Rolle-Aubonne
UPDATE TIERS SET DISTRICT_FISCAL_ID = 13, REGION_FISCALE_ID = null WHERE NUMERO_CA = 17;
--Vevey
UPDATE TIERS SET DISTRICT_FISCAL_ID = 14, REGION_FISCALE_ID = 3 WHERE NUMERO_CA = 18;
--Yverson
UPDATE TIERS SET DISTRICT_FISCAL_ID = 15, REGION_FISCALE_ID = 4 WHERE NUMERO_CA = 19;

--
-- [SIFISC-2337] Nouveau format de la table immeuble
--
drop table IMMEUBLE cascade constraints;
create table IMMEUBLE (id number(19,0) not null, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0), DATE_FIN number(10,0), ESTIMATION_FISCALE number(10,0) not null, GENRE_PROPRIETE nvarchar2(12) not null, LIEN_RF nvarchar2(255), NATURE_IMMEUBLE nvarchar2(255) not null, NOM_COMMUNE nvarchar2(255) not null, NUMERO_IMMEUBLE nvarchar2(20) not null, PART_PROPRIETE_DENOMINATEUR number(10,0) not null, PART_PROPRIETE_NUMERATEUR number(10,0) not null, REF_ESTIM_FISC nvarchar2(255), CTB_ID number(19,0) not null, primary key (id));
create index IDX_IMM_CTB_ID on IMMEUBLE (CTB_ID);
alter table IMMEUBLE add constraint FK_IMM_CTB_ID foreign key (CTB_ID) references TIERS;

--
-- Nouveaux indexes proposés par le CEI
--
CREATE INDEX IDX_EVT_CIVIL_ERR_EVT_ID ON EVENEMENT_CIVIL_ERREUR(EVT_CIVIL_ID);
CREATE INDEX IDX_EVT_EXTRNE_CORR_ID ON EVENEMENT_EXTERNE(CORRELATION_ID);
CREATE INDEX IDX_MVT_DOSSIER_BORD_ID on MOUVEMENT_DOSSIER (BORDEREAU_ID);		-- celui-ci a été oublié dans un script d'update précédent...
CREATE INDEX IDX_TACHE_TYPE_CA ON TACHE(TACHE_TYPE, ETAT, ANNULATION_DATE, DATE_ECHEANCE, CA_ID);

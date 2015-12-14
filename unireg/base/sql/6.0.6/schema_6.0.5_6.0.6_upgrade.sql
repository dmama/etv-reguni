-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.6', '6.0.5_6.0.6_upgrade');

-- Nouvelle colonne dans la table des tâches pour la catégorie d'entreprise
ALTER TABLE TACHE ADD CATEGORIE_ENTREPRISE NVARCHAR2(10);

--
-- Ajout de la modélisation nécessaire à la gestion des demandes de délai (en particulier "refusables")
--

ALTER TABLE DELAI_DECLARATION ADD (ETAT NVARCHAR2(10), SURSIS NUMBER(1,0));
UPDATE DELAI_DECLARATION SET ETAT='ACCORDE', SURSIS=0;
ALTER TABLE DELAI_DECLARATION MODIFY (ETAT NVARCHAR2(10) NOT NULL, SURSIS NUMBER(1,0) NOT NULL);
ALTER TABLE DELAI_DECLARATION ADD CLE_ARCHIVAGE_COURRIER NVARCHAR2(40);
UPDATE DELAI_DECLARATION SET CLE_ARCHIVAGE_COURRIER=LPAD(MOD(ID,1000000),6,'0') || ' ' || RPAD('Confirmation Delai', 19, ' ') || ' ' || TO_CHAR(LOG_CDATE, 'MMDDHH24MISSFF3') WHERE CONFIRMATION_ECRITE=1;
ALTER TABLE DELAI_DECLARATION DROP COLUMN CONFIRMATION_ECRITE;

--
-- Renommage du paramètre du délai de retour des DI émises manuellement pour laisser la place à l'équivalent PM
--

UPDATE PARAMETRE SET NOM='delaiRetourDeclarationImpotPPEmiseManuellement', LOG_MDATE=CURRENT_DATE, LOG_MUSER='[system-sipm]' WHERE NOM='delaiRetourDeclarationImpotEmiseManuellement';

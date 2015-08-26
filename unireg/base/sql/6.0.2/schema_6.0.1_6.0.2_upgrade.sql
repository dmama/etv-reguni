-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.2', '6.0.1_6.0.2_upgrade');

-- La séquence S_PM devient S_CAAC (elle gère en fait les Collectivités Administratives et les Autres Communautés...)
RENAME S_PM TO S_CAAC;

-- La nouvelle séquence S_PM gère les entreprises
CREATE SEQUENCE S_PM START WITH 80000 INCREMENT BY 1;

-- Renommage du paramètre "premierePeriodeFiscale" pour distinguer entre le cas des personnes physiques et celui des personnes morales
UPDATE PARAMETRE SET NOM='premierePeriodeFiscalePersonnesPhysiques', LOG_MDATE=CURRENT_DATE, LOG_MUSER='[system-sipm]' WHERE NOM='premierePeriodeFiscale';

-- Ajout de la devise associée au capital de l'entreprise
ALTER TABLE DONNEES_RC ADD (MONNAIE_CAPITAL NVARCHAR2(3));

-- Ajout de la raison sociale propre aux établissement
ALTER TABLE TIERS ADD (ETB_RAISON_SOCIALE NVARCHAR2(250));

-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.6', '6.0.5_6.0.6_upgrade');

-- Nouvelle colonne dans la table des tâches pour la catégorie d'entreprise
ALTER TABLE TACHE ADD CATEGORIE_ENTREPRISE NVARCHAR2(10);
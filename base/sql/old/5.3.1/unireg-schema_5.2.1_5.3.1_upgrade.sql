-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.3.1', '5.2.1_5.3.1_upgrade');

-- [SIFISC-6514] Ajout d'une colonne texte pour stocker l'origine d'un non-habitant
ALTER TABLE TIERS add NH_LIBELLE_COMMUNE_ORIGINE NVARCHAR2(250);

-- [SIFISC-7179] Ajout d'une colonne texte dans l'événement d'identification de contribuable pour stocker les métadonnées du message entrant
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD META_DATA NVARCHAR2(1023);


--[SIFISC-6951] Suppression des colonnes taux d'activité et type d'activité pour les rapports de travail
ALTER TABLE RAPPORT_ENTRE_TIERS DROP COLUMN TAUX_ACTIVITE;

ALTER TABLE RAPPORT_ENTRE_TIERS DROP COLUMN TYPE_ACTIVITE;

-- [SIFISC-7168] On oublie la sauvegarde de l'adresse mail e-facture
ALTER TABLE TIERS DROP COLUMN ADRESSE_EMAIL_EFACTURE;

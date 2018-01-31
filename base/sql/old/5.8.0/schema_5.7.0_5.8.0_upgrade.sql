-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.8.0', '5.7.0_5.8.0_upgrade');

-- Ajout des motifs de rattachement PHS et EFF (la longueur passe de 22 à 26)
ALTER TABLE FOR_FISCAL MODIFY MOTIF_RATTACHEMENT nvarchar2(26);
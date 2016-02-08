-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('6.0.8', '6.0.7_6.0.8_upgrade');

-- le rapport d'administration d'entreprise a besoin d'un attribut supplémentaire (rôle de 'président')
ALTER TABLE RAPPORT_ENTRE_TIERS ADD ADMIN_PRESIDENT NUMBER(1,0);


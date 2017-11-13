-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.3.0', '7.2.3bis_7.3.0_upgrade');

-- SIFISC-24752 : augmenté les valeurs maximales sur les données des allégements fiscaux
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_LOC_REVENU NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_LOC_VOLUME NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_LOC_SURFACE NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_PRUS_REVENU NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_PRUS_VOLUME NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_PRUS_SURFACE NUMBER(19);
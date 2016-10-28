-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.5.0', '5.4.0_5.5.0_upgrade');

-- SIFISC-8427: nouvel index sur l'événement référencé par un événement civil eCH
create index IDX_EV_CIV_ECH_REF on EVENEMENT_CIVIL_ECH (REF_MESSAGE_ID);
-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.7.0', '5.6.0_5.7.0_upgrade');

-- [SIFISC-9908] Ajout d'un paramètre booléen sur les périodes fiscales pour demander l'affichage ou pas du code de contrôle sur les sommations de DI de cette période
ALTER TABLE PERIODE_FISCALE ADD CODE_CTRL_SOMM_DI NUMBER(1,0);
UPDATE PERIODE_FISCALE SET CODE_CTRL_SOMM_DI=0;
ALTER TABLE PERIODE_FISCALE MODIFY CODE_CTRL_SOMM_DI NUMBER(1,0) NOT NULL;
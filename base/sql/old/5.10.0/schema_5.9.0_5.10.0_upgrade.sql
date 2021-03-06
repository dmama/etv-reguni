-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('5.10.0', '5.9.0_5.10.0_upgrade');

-- SIFISC-12815
ALTER TABLE EVENEMENT_IDENTIFICATION_CTB ADD COMMENTAIRE_TRAITEMENT nvarchar2(255);

-- SIFISC-13351
ALTER TABLE TIERS ADD NH_LIBELLE_ORIGINE NVARCHAR2(100);
ALTER TABLE TIERS ADD NH_CANTON_ORIGINE NVARCHAR2(2);
ALTER TABLE TIERS ADD NH_NOM_NAISSANCE NVARCHAR2(250);
ALTER TABLE REQDES_PARTIE_PRENANTE ADD NOM_NAISSANCE NVARCHAR2(100);
ALTER TABLE REQDES_PARTIE_PRENANTE ADD LIBELLE_ORIGINE NVARCHAR2(50);
ALTER TABLE REQDES_PARTIE_PRENANTE ADD CANTON_ORIGINE NVARCHAR2(2);

-- SIFISC-13397
ALTER TABLE REQDES_PARTIE_PRENANTE ADD NO_CTB_CREE NUMBER(19,0);
ALTER TABLE REQDES_PARTIE_PRENANTE ADD CONSTRAINT FK_REQDES_PP_CTB_CREE FOREIGN KEY (NO_CTB_CREE) REFERENCES TIERS;

--SIFISC-12624
CREATE TABLE DECISION_ACI (id number(19,0) NOT NULL, ANNULATION_DATE timestamp, ANNULATION_USER nvarchar2(65), LOG_CDATE timestamp, LOG_CUSER nvarchar2(65), LOG_MDATE timestamp, LOG_MUSER nvarchar2(65), DATE_DEBUT number(10,0) not null, DATE_FIN number(10,0),NUMERO_OFS number(10,0) not null, TYPE_AUT_FISC nvarchar2(22) not null, REMARQUE nvarchar2(2000), TIERS_ID number(19,0) not null, primary key (id));
CREATE index IDX_DECISION_ACI_TIERS_ID on DECISION_ACI(TIERS_ID);
ALTER TABLE DECISION_ACI ADD CONSTRAINT FK_DECISION_ACI_TRS_ID FOREIGN KEY (TIERS_ID) REFERENCES TIERS;

-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('18R3.A', '18R2.C_18R3.A_upgrade');

-- [IMM-795] historisation des liens servitudes-immeubles
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD ID NUMBER(19,0);
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD ANNULATION_DATE TIMESTAMP;
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD ANNULATION_USER NVARCHAR2(65);
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD LOG_CDATE TIMESTAMP;
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD LOG_CUSER NVARCHAR2(65);
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD LOG_MDATE TIMESTAMP;
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD LOG_MUSER NVARCHAR2(65);
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD DATE_DEBUT NUMBER(10,0);
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD DATE_FIN NUMBER(10,0);

-- gestion de l'id
ALTER TABLE RF_SERVITUDE_IMMEUBLE DROP PRIMARY KEY;
UPDATE RF_SERVITUDE_IMMEUBLE SET ID = hibernate_sequence.nextval;
ALTER TABLE RF_SERVITUDE_IMMEUBLE MODIFY ID NOT NULL;
ALTER TABLE RF_SERVITUDE_IMMEUBLE ADD CONSTRAINT PK_SERVITUDE_IMMEUBLE_ID PRIMARY KEY (ID);

-- rattrapage des dates de début et de fin
UPDATE RF_SERVITUDE_IMMEUBLE SET LOG_CUSER = 'SQL-IMM-795';
UPDATE RF_SERVITUDE_IMMEUBLE SET LOG_CDATE = CURRENT_DATE;
UPDATE RF_SERVITUDE_IMMEUBLE SET LOG_MUSER = 'SQL-IMM-795';
UPDATE RF_SERVITUDE_IMMEUBLE SET LOG_MDATE = CURRENT_DATE;
UPDATE RF_SERVITUDE_IMMEUBLE SET DATE_DEBUT = (select d.DATE_DEBUT_METIER from RF_DROIT d where d.id = DROIT_ID);
UPDATE RF_SERVITUDE_IMMEUBLE SET DATE_FIN = (select d.DATE_FIN_METIER from RF_DROIT d where d.id = DROIT_ID);

-- [IMM-795] historisation des liens servitudes-ayants-droits
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD ID NUMBER(19,0);
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD ANNULATION_DATE TIMESTAMP;
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD ANNULATION_USER NVARCHAR2(65);
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD LOG_CDATE TIMESTAMP;
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD LOG_CUSER NVARCHAR2(65);
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD LOG_MDATE TIMESTAMP;
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD LOG_MUSER NVARCHAR2(65);
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD DATE_DEBUT NUMBER(10,0);
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD DATE_FIN NUMBER(10,0);

-- gestion de l'id
ALTER TABLE RF_SERVITUDE_AYANT_DROIT DROP PRIMARY KEY;
UPDATE RF_SERVITUDE_AYANT_DROIT SET ID = hibernate_sequence.nextval;
ALTER TABLE RF_SERVITUDE_AYANT_DROIT MODIFY ID NOT NULL;
ALTER TABLE RF_SERVITUDE_AYANT_DROIT ADD CONSTRAINT PK_SERVITUDE_AYANT_DROIT_ID PRIMARY KEY (ID);

-- rattrapage des dates de début et de fin
UPDATE RF_SERVITUDE_AYANT_DROIT SET LOG_CUSER = 'SQL-IMM-795';
UPDATE RF_SERVITUDE_AYANT_DROIT SET LOG_CDATE = CURRENT_DATE;
UPDATE RF_SERVITUDE_AYANT_DROIT SET LOG_MUSER = 'SQL-IMM-795';
UPDATE RF_SERVITUDE_AYANT_DROIT SET LOG_MDATE = CURRENT_DATE;
UPDATE RF_SERVITUDE_AYANT_DROIT SET DATE_DEBUT = (select d.DATE_DEBUT_METIER from RF_DROIT d where d.id = DROIT_ID);
UPDATE RF_SERVITUDE_AYANT_DROIT SET DATE_FIN = (select d.DATE_FIN_METIER from RF_DROIT d where d.id = DROIT_ID);


-- [SIFISC-28816] déplacement de la blacklist en DB
CREATE TABLE RF_BLACKLIST(
    ID NUMBER(19,0) NOT NULL,
    TYPE_ENTITE NVARCHAR2(14) NOT NULL,
    ID_RF NVARCHAR2(37) NOT NULL,
    REASON NVARCHAR2(255) NOT NULL,
    PRIMARY KEY (ID)
);
CREATE UNIQUE INDEX IDX_BLACKLIST_TYPE_ID_RF ON RF_BLACKLIST (TYPE_ENTITE, ID_RF);

INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810108101381012b3d64cb4', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd5c83f8a', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd5c83f8e', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd6404147', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd6404148', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd640414c', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd640414d', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd640414e', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd640414f', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd641415b', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd5c83f86', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd6414157', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd641415f', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd6414160', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd6404149', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd640414a', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd640414b', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd6414152', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd6414153', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd6414154', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd754430d', 'import initial (SIFISC-28816)');
INSERT INTO RF_BLACKLIST(ID, TYPE_ENTITE, ID_RF, REASON) VALUES (hibernate_sequence.nextval, 'IMMEUBLE', '_1f1091523810190f0138101cd7564338', 'import initial (SIFISC-28816)');

-- [SIFISC-20035] historisation des coordonnées financières
create table COORDONNEE_FINANCIERE (
    ID NUMBER(19) not null primary key,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    BIC_SWIFT NVARCHAR2(15),
    IBAN NVARCHAR2(34),
    TITULAIRE NVARCHAR2(200),
    TIERS_ID NUMBER(19) not null constraint FK_COORDFIN_TIERS_ID references TIERS
);
create index IDX_COORDFIN_TIERS_ID on COORDONNEE_FINANCIERE (TIERS_ID);

-- on migre les coordonnées existantes
insert into COORDONNEE_FINANCIERE (id, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, DATE_FIN, BIC_SWIFT, IBAN, TITULAIRE, TIERS_ID)
    select
        HIBERNATE_SEQUENCE.nextval,
        CURRENT_DATE,
        'SQL-SIFISC-20035',
        CURRENT_DATE,
        'SQL-SIFISC-20035',
        null,
        null,
        ADRESSE_BIC_SWIFT,
        NUMERO_COMPTE_BANCAIRE,
        TITULAIRE_COMPTE_BANCAIRE,
        NUMERO
    from TIERS
    where ADRESSE_BIC_SWIFT is not null or NUMERO_COMPTE_BANCAIRE is not null or TITULAIRE_COMPTE_BANCAIRE is not null;
--     where ADRESSE_BIC_SWIFT is not null or NUMERO_COMPTE_BANCAIRE != 'CH' or TITULAIRE_COMPTE_BANCAIRE is not null;

-- on supprime les colonnes plus utilisées
ALTER TABLE TIERS DROP COLUMN ADRESSE_BIC_SWIFT;
ALTER TABLE TIERS DROP COLUMN NUMERO_COMPTE_BANCAIRE;
ALTER TABLE TIERS DROP COLUMN TITULAIRE_COMPTE_BANCAIRE;

-- [SIFISC-29029] migration du numéro d'opérateur Host au visa IAM
ALTER TABLE DROIT_ACCES MODIFY NUMERO_IND_OPER DEFAULT NULL NULL;
ALTER TABLE DROIT_ACCES ADD VISA_OPERATEUR NVARCHAR2(25);
CREATE INDEX IDX_VISA_OPERATEUR ON DROIT_ACCES (VISA_OPERATEUR ASC);
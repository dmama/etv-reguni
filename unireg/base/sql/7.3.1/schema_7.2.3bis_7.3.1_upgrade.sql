-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.3.1', '7.2.3bis_7.3.1_upgrade');

-- SIFISC-24752 : augmenté les valeurs maximales sur les données des allégements fiscaux
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_LOC_REVENU NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_LOC_VOLUME NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_LOC_SURFACE NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_PRUS_REVENU NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_PRUS_VOLUME NUMBER(19);
ALTER TABLE ALLEGEMENT_FONCIER MODIFY DEG_PRUS_SURFACE NUMBER(19);
-- SIFISC-24595 : ajout des principaux de communautés
create table RF_MODELE_COMMUNAUTE
(
    ID NUMBER(19) not null
        primary key,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    MEMBRES_HASH_CODE NUMBER(10)
);
create index IDX_MODCOMM_HASHCODE on RF_MODELE_COMMUNAUTE (MEMBRES_HASH_CODE);

create table RF_MEMBRE_COMMUNAUTE
(
    MODEL_COMMUNAUTE_ID NUMBER(19) not null
        constraint FK_MEMCOMM_MODEL_ID
        references RF_MODELE_COMMUNAUTE,
    AYANT_DROIT_ID NUMBER(19) not null
        constraint FK_MEMCOMM_AYANTDROIT_ID
        references RF_AYANT_DROIT,
    primary key (MODEL_COMMUNAUTE_ID, AYANT_DROIT_ID)
);

create table RF_PRINCIPAL_COMMUNAUTE
(
    ID NUMBER(19) not null
        primary key,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10) not null,
    DATE_FIN NUMBER(10),
    PRINCIPAL_ID NUMBER(19) not null
        constraint FK_PRINCIPAL_ID
        references RF_AYANT_DROIT,
    MODEL_COMMUNAUTE_ID NUMBER(19) not null
        constraint FK_PRINC_MODCOMM_ID
        references RF_MODELE_COMMUNAUTE
);
create index IDX_PRINC_PRINCIPAL_ID on RF_PRINCIPAL_COMMUNAUTE (PRINCIPAL_ID);
create index IDX_PRINC_MODCOMM_ID on RF_PRINCIPAL_COMMUNAUTE (MODEL_COMMUNAUTE_ID);

create table RF_REGROUPEMENT_COMMUNAUTE
(
    ID NUMBER(19) not null
        primary key,
    ANNULATION_DATE TIMESTAMP(6),
    ANNULATION_USER NVARCHAR2(65),
    LOG_CDATE TIMESTAMP(6),
    LOG_CUSER NVARCHAR2(65),
    LOG_MDATE TIMESTAMP(6),
    LOG_MUSER NVARCHAR2(65),
    DATE_DEBUT NUMBER(10),
    DATE_FIN NUMBER(10),
    COMMUNAUTE_ID NUMBER(19) not null
        constraint FK_REGRCOMM_RF_COMMUNAUTE_ID
        references RF_AYANT_DROIT,
    MODEL_ID NUMBER(19) not null
        constraint FK_REGRCOMM_RF_MODEL_ID
        references RF_MODELE_COMMUNAUTE
);
create index IDX_REGRCOMM_RF_COMMUNAUTE_ID on RF_REGROUPEMENT_COMMUNAUTE (COMMUNAUTE_ID);
create index IDX_REGRCOMM_RF_MODEL_ID on RF_REGROUPEMENT_COMMUNAUTE (MODEL_ID);

-- SIFISC-24595 : ajout des événements fiscaux sur les communautés
ALTER TABLE EVENEMENT_FISCAL ADD COMMUNAUTE_ID NUMBER(19) NULL;
ALTER TABLE EVENEMENT_FISCAL ADD TYPE_EVT_COMMUNAUTE NVARCHAR2(20) NULL;
ALTER TABLE EVENEMENT_FISCAL ADD CONSTRAINT FK_EVTFISC_COMMUNAUTE_ID FOREIGN KEY (COMMUNAUTE_ID) REFERENCES RF_AYANT_DROIT (ID);
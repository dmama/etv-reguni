-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('19R2.A', '19R1.C_19R2.A_upgrade');

create table LIBERATION_DOCUMENT_FISCAL (
       ID                     number(19, 0) not null primary key ,
       LIBERATION_TYPE        nvarchar2(31) not null,
       ANNULATION_DATE        timestamp,
       ANNULATION_USER        nvarchar2(65),
       LOG_CDATE              timestamp,
       LOG_CUSER              nvarchar2(65),
       LOG_MDATE              timestamp,
       LOG_MUSER              nvarchar2(65),
       DATE_LIBERATION        number(10, 0),
       MOTIF_LIBERATION       nvarchar2(256),
       DOCUMENT_FISCAL_ID     number(19, 0) not null
);
CREATE INDEX IDX_LIB_DOCFISC_DOCFISC_ID on LIBERATION_DOCUMENT_FISCAL (DOCUMENT_FISCAL_ID);
ALTER TABLE LIBERATION_DOCUMENT_FISCAL ADD CONSTRAINt FK_LIB_DOCFISC_DOCFISC_ID FOREIGN KEY (DOCUMENT_FISCAL_ID) references DOCUMENT_FISCAL;
ALTER TABLE LIBERATION_DOCUMENT_FISCAL ADD MSG_LIB_BUSINESSID nvarchar2(256) ;

COMMENT ON TABLE LIBERATION_DOCUMENT_FISCAL IS 'Table des informations de demande de liberation des documents fiscaux';
COMMENT ON COLUMN LIBERATION_DOCUMENT_FISCAL.MSG_LIB_BUSINESSID IS 'stocke le business ID du message de liberation envoyé dans l''ESB';
COMMENT ON COLUMN LIBERATION_DOCUMENT_FISCAL.MOTIF_LIBERATION IS 'Indique la motivation de la demande de liberation';
COMMENT ON COLUMN LIBERATION_DOCUMENT_FISCAL.DATE_LIBERATION IS 'Indique la date de la demande de liberation';
COMMENT ON COLUMN LIBERATION_DOCUMENT_FISCAL.LIBERATION_TYPE IS 'Indique le type de demande de liberation';

--SIFISC-29729 Supprimer les anciens numéros d'opérateurs
ALTER TABLE MOUVEMENT_DOSSIER DROP COLUMN NUMERO_INDIVIDU;
ALTER TABLE DROIT_ACCES DROP COLUMN NUMERO_IND_OPER;
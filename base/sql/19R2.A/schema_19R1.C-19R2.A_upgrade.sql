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
create index IDX_LIB_DOCFISC_DOCFISC_ID on LIBERATION_DOCUMENT_FISCAL (DOCUMENT_FISCAL_ID);
alter table LIBERATION_DOCUMENT_FISCAL add constraint FK_LIB_DOCFISC_DOCFISC_ID foreign key (DOCUMENT_FISCAL_ID) references DOCUMENT_FISCAL;
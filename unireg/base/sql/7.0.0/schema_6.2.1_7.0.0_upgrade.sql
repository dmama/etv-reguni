-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('7.0.0', '6.2.1_7.0.0_upgrade');

--
-- [SIFISC-20372] Import des immeubles du RF
--

create table EVENEMENT_RF_IMMEUBLE (
	id number(19,0) not null,
	ANNULATION_DATE timestamp,
	ANNULATION_USER nvarchar2(65),
	LOG_CDATE timestamp,
	LOG_CUSER nvarchar2(65),
	LOG_MDATE timestamp,
	LOG_MUSER nvarchar2(65),
	DATE_EVENEMENT number(10,0),
	ETAT nvarchar2(10),
	FILE_URL nvarchar2(255),
	ERROR_MESSAGE blob,
	primary key (id)
);
create index IDX_EV_RF_IMM_ETAT on EVENEMENT_RF_IMMEUBLE (ETAT);

-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('19R1.B', '19R1.A_19R1.B_upgrade');

--FISCPROJ-964

-- initialisation des paramètres de délais pour la période fiscale 2019
insert into PARAMETRE_PERIODE_FISCALE(PPF_TYPE, ID, PERIODE_ID, TYPE_TIERS, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'ONLINE', HIBERNATE_SEQUENCE.NEXTVAL, pf.ID, 'PP', 'SQL-FISCPROJ-963', CURRENT_DATE, 'SQL-FISCPROJ-963', CURRENT_DATE
from PERIODE_FISCALE pf
where pf.ANNEE = 2019;

insert into PARAMETRE_PERIODE_FISCALE(PPF_TYPE, ID, PERIODE_ID, TYPE_TIERS, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'ONLINE', HIBERNATE_SEQUENCE.NEXTVAL, pf.ID, 'PM', 'SQL-FISCPROJ-963', CURRENT_DATE, 'SQL-FISCPROJ-963', CURRENT_DATE
from PERIODE_FISCALE pf
where pf.ANNEE = 2019;

insert into PARAMETRE_DELAIS_ONLINE(DISCRIMINATOR, ID, PARAM_PF_DELAI_ID, DATE_DEBUT, DATE_FIN, UNITAIRE_PP, GROUPEE_PP, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'DI_PP', HIBERNATE_SEQUENCE.NEXTVAL, ppf.ID, 20200101, 20200515, '0630', '0630', 'SQL-FISCPROJ-963', CURRENT_DATE, 'SQL-FISCPROJ-963', CURRENT_DATE
from PARAMETRE_PERIODE_FISCALE ppf
where ppf.PPF_TYPE = 'ONLINE' and ppf.TYPE_TIERS = 'PP' and ppf.PERIODE_ID = (select ID from PERIODE_FISCALE pf where pf.ANNEE = 2019);

insert into PARAMETRE_DELAIS_ONLINE(DISCRIMINATOR, ID, PARAM_PF_DELAI_ID, DATE_DEBUT, DATE_FIN, UNITAIRE_PP, GROUPEE_PP, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'DI_PP', HIBERNATE_SEQUENCE.NEXTVAL, ppf.ID, 20200516, 20200615, '0630, 0930', '0630, 0930', 'SQL-FISCPROJ-963', CURRENT_DATE, 'SQL-FISCPROJ-963', CURRENT_DATE
from PARAMETRE_PERIODE_FISCALE ppf
where ppf.PPF_TYPE = 'ONLINE' and ppf.TYPE_TIERS = 'PP' and ppf.PERIODE_ID = (select ID from PERIODE_FISCALE pf where pf.ANNEE = 2019);

insert into PARAMETRE_DELAIS_ONLINE(DISCRIMINATOR, ID, PARAM_PF_DELAI_ID, DATE_DEBUT, DATE_FIN, UNITAIRE_PP, GROUPEE_PP, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'DI_PP', HIBERNATE_SEQUENCE.NEXTVAL, ppf.ID, 20200616, 20200831, '0930', null, 'SQL-FISCPROJ-963', CURRENT_DATE, 'SQL-FISCPROJ-963', CURRENT_DATE
from PARAMETRE_PERIODE_FISCALE ppf
where ppf.PPF_TYPE = 'ONLINE' and ppf.TYPE_TIERS = 'PP' and ppf.PERIODE_ID = (select ID from PERIODE_FISCALE pf where pf.ANNEE = 2019);

insert into PARAMETRE_DELAIS_ONLINE(DISCRIMINATOR, ID, PARAM_PF_DELAI_ID, DATE_DEBUT, DATE_FIN, UNITAIRE_PP, GROUPEE_PP, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'DI_PP', HIBERNATE_SEQUENCE.NEXTVAL, ppf.ID, 20200901, 20201231, null, null, 'SQL-FISCPROJ-963', CURRENT_DATE, 'SQL-FISCPROJ-963', CURRENT_DATE
from PARAMETRE_PERIODE_FISCALE ppf
where ppf.PPF_TYPE = 'ONLINE' and ppf.TYPE_TIERS = 'PP' and ppf.PERIODE_ID = (select ID from PERIODE_FISCALE pf where pf.ANNEE = 2019);

insert into PARAMETRE_DELAIS_ONLINE(DISCRIMINATOR, ID, PARAM_PF_DELAI_ID, INDEX_PERIODE, DELAI_DEBUT, UNITAIRE_PM, GROUPEE_PM, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'DI_PM', HIBERNATE_SEQUENCE.NEXTVAL, ppf.ID, 0, '0M', '6M + 75D', '6M + 75D', 'SQL-FISCPROJ-963', CURRENT_DATE, 'SQL-FISCPROJ-963', CURRENT_DATE
from PARAMETRE_PERIODE_FISCALE ppf
where ppf.PPF_TYPE = 'ONLINE' and ppf.TYPE_TIERS = 'PM' and ppf.PERIODE_ID = (select ID from PERIODE_FISCALE pf where pf.ANNEE = 2019);

insert into PARAMETRE_DELAIS_ONLINE(DISCRIMINATOR, ID, PARAM_PF_DELAI_ID, INDEX_PERIODE, DELAI_DEBUT, UNITAIRE_PM, GROUPEE_PM, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'DI_PM', HIBERNATE_SEQUENCE.NEXTVAL, ppf.ID, 1, '6M', '6M + 75D, 12M', '6M + 75D, 12M', 'SQL-FISCPROJ-963', CURRENT_DATE, 'SQL-FISCPROJ-963', CURRENT_DATE
from PARAMETRE_PERIODE_FISCALE ppf
where ppf.PPF_TYPE = 'ONLINE' and ppf.TYPE_TIERS = 'PM' and ppf.PERIODE_ID = (select ID from PERIODE_FISCALE pf where pf.ANNEE = 2019);

insert into PARAMETRE_DELAIS_ONLINE(DISCRIMINATOR, ID, PARAM_PF_DELAI_ID, INDEX_PERIODE, DELAI_DEBUT, UNITAIRE_PM, GROUPEE_PM, LOG_CUSER, LOG_CDATE, LOG_MUSER, LOG_MDATE)
select 'DI_PM', HIBERNATE_SEQUENCE.NEXTVAL, ppf.ID, 2, '9M', null, null, 'FISCPROJ-963', CURRENT_DATE, 'FISCPROJ-963', CURRENT_DATE
from PARAMETRE_PERIODE_FISCALE ppf
where ppf.PPF_TYPE = 'ONLINE' and ppf.TYPE_TIERS = 'PM' and ppf.PERIODE_ID = (select ID from PERIODE_FISCALE pf where pf.ANNEE = 2019);

UPDATE PARAMETRE SET NOM='delaiImpressionCadev' WHERE NOM='delaiCadevImpressionQuestionnaireSNC';

-- [FISCPROJ-985] correction du type de délai (implicite/explicite)
update DELAI_DOCUMENT_FISCAL set TYPE_DELAI = 'EXPLICITE' where DISCRIMINATOR = 'DELAI_DECLARATION' and LOG_MUSER = 'DemandeDelaiCollectiveJob';

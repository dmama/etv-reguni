alter table TIERS add (DPI_NOM1 varchar2(250 char), DPI_NOM2 varchar2(250 char));
alter table TIERS add (DATE_DECES decimal(10));
update TIERS set DATE_DECES = NH_DATE_DECES;
alter table TIERS drop column NH_DATE_DECES;
alter table TIERS add (PP_HABITANT decimal(1));
update TIERS set PP_HABITANT = 1, TIERS_TYPE = 'PersonnePhysique' where TIERS_TYPE = 'Habitant';
update TIERS set PP_HABITANT = 0, TIERS_TYPE = 'PersonnePhysique' where TIERS_TYPE = 'NonHabitant';

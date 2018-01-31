alter table DECLARATION modify TIERS_ID not null;

alter table DELAI_DECLARATION modify DECLARATION_ID not null;

delete from ETAT_DECLARATION where DECLARATION_ID is null;

alter table ETAT_DECLARATION modify DECLARATION_ID not null;

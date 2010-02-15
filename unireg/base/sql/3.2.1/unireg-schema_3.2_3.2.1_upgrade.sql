alter table DECLARATION modify TIERS_ID not null;

alter table DELAI_DECLARATION modify DECLARATION_ID not null;

--TODO (msi/jde/gdy) vérifier que les état avec fk nulles en prod correspondent bien aux DIs supprimées par le patch de correction
delete from ETAT_DECLARATION where DECLARATION_ID is null;

alter table ETAT_DECLARATION modify DECLARATION_ID not null;

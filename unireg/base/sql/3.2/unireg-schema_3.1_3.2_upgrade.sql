alter table IDENTIFICATION_PERSONNE modify (IDENTIFIANT varchar2(13 char));
alter table EVENEMENT_CIVIL_ERREUR modify (MESSAGE varchar2(1014 char));
alter table TIERS add (NH_NO_OFS_COMMUNE_ORIGINE number(10,0));
alter table TIERS rename column NH_NO_OFS_PAYS_ORIGINE to NH_NO_OFS_NATIONALITE;
create index IDX_ET_DI_SOMM_DI on ETAT_DECLARATION (DECLARATION_ID, ANNULATION_DATE, TYPE, DATE_OBTENTION);
create index IDX_ANC_NO_SRC on TIERS (ANCIEN_NUMERO_SOURCIER);
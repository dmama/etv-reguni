alter table DECLARATION add (SANS_RAPPEL decimal(1));
alter table TIERS add (NH_NO_OFS_COMMUNE_ORIGINE number(10,0));
alter table TIERS rename column NH_NO_OFS_PAYS_ORIGINE to NH_NO_OFS_NATIONALITE;
create index IDX_ANC_NO_SRC on TIERS (ANCIEN_NUMERO_SOURCIER);
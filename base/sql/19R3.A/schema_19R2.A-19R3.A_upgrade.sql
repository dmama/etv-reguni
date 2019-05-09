-- Version
INSERT INTO VERSION_DB (VERSION_NB, SCRIPT_ID) VALUES ('19R3.A', '19R2.A_19R3.A_upgrade');

-- [SIFISC-31076] Passage à Hibernate 5 : renommage des foreign keys par défaut

-- note : les noms des contraintes par défaut changent d'un environnement à l'autre (certainement un side-effect
--        des exports-imports Chronobase), on ne peut donc pas se baser sur leurs noms pour les renommer
--        (il s'agit des contraintes SYS_C0015706 et SYS_C0015707 en production)
begin
    for r in ( select table_name, constraint_name
               from user_constraints
               where constraint_type = 'R'
                 and table_name = 'RF_SITUATION' )
        loop
            execute immediate 'alter table '|| r.table_name ||' drop constraint '|| r.constraint_name;
        end loop;
end;

alter index IDX_SITUATION_RF_COMMUNE_ID rename to IDX_SIT_RF_COMMUNE_ID;
alter table RF_SITUATION add constraint FK_SIT_RF_COMMUNE_ID foreign key (COMMUNE_ID) references RF_COMMUNE;
alter table RF_SITUATION add constraint FK_SIT_RF_IMMEUBLE_ID foreign key (IMMEUBLE_ID) references RF_IMMEUBLE;

update RAPPORT_ENTRE_TIERS set RAPPORT_ENTRE_TIERS_TYPE = 'AppartenanceMenage' where TYPE = 'APPARTENANCE_MENAGE';
update RAPPORT_ENTRE_TIERS set RAPPORT_ENTRE_TIERS_TYPE = 'RapportPrestationImposable' where TYPE = 'PRESTATION_IMPOSABLE';
update RAPPORT_ENTRE_TIERS set RAPPORT_ENTRE_TIERS_TYPE = 'RepresentationConventionnelle' where TYPE = 'REPRESENTATION';
update RAPPORT_ENTRE_TIERS set RAPPORT_ENTRE_TIERS_TYPE = 'ConseilLegal' where TYPE = 'CONSEIL_LEGAL';
update RAPPORT_ENTRE_TIERS set RAPPORT_ENTRE_TIERS_TYPE = 'Curatelle' where TYPE = 'CURATELLE';
update RAPPORT_ENTRE_TIERS set RAPPORT_ENTRE_TIERS_TYPE = 'Tutelle' where TYPE = 'TUTELLE';
alter table RAPPORT_ENTRE_TIERS drop column TYPE;
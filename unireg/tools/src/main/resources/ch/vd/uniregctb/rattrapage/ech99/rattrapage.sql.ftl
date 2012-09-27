<#macro SchemaPrefix>UNIREG.</#macro>
--------------------------------------------------
-- Rattrapage des événements civils émis par ech99
-- Nombre total d'événements: ${TOTAL}
-- script généré le: ${DATE?datetime}
--------------------------------------------------
<#list LIST_OF_IDS as IDS>
update
  <@SchemaPrefix/>EVENEMENT_CIVIL_ECH
set
  etat='FORCE',
  log_mdate = CURRENT_TIMESTAMP,
  log_muser = '${USER}',
  commentaire_traitement = 'Forçage automatique de l''événement issue d''un ech99: reindexation des données civils du contribuable'
where
  id in (${IDS})
  and etat = 'EN_ERREUR'
  and annulation_date is null;

update
  <@SchemaPrefix/>TIERS t
set
  index_dirty = 1,
  log_mdate = CURRENT_TIMESTAMP,
  log_muser = '${USER}'
where
  annulation_date is null
  and exists (
    select id
    from <@SchemaPrefix/>EVENEMENT_CIVIL_ECH e
    where
	  t.numero_individu = e.no_individu
      and e.id in (${IDS})
      and e.etat = 'FORCE');

</#list>
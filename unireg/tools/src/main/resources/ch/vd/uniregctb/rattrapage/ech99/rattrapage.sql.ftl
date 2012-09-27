<#macro SchemaPrefix></#macro>

<#list EVT_IDS as ID>
-----------------------------------
-- Rattrapage des evenements: ${ID}
-----------------------------------

update
  <@SchemaPrefix/>EVENEMENT_CIVIL_ECH
set
  etat='TRAITE',
  log_mdate = CURRENT_TIMESTAMP,
  log_muser = '${USER}',
  commentaire_traitement = 'Traitement automatique de l''evenement issue d''un ech99: reindexation des donnees civils du contribuable'
where
  id in (${ID})
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
      and e.id in (${ID})
      and e.etat = 'TRAITE');

</#list>
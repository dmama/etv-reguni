<#macro SchemaPrefix>UNIREG.</#macro>
--------------------------------------------------
--           Rattrapage SQL ech99
--
-- Forçage des événements issus d'un ech99
-- + réindexation des individus concernés
--
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
  commentaire_traitement = 'Forçage automatique de l''événement issu d''un eCH-0099 : réindexation des données civiles du contribuable'
where
  id in (${IDS})
  and etat IN ('EN_ERREUR', 'EN_ATTENTE')
  and annulation_date is null;

update
  <@SchemaPrefix/>TIERS
set
  index_dirty = 1,
  log_mdate = CURRENT_TIMESTAMP,
  log_muser = '${USER}'
where
  annulation_date is null
  and numero_individu in (
  	select no_individu
  	from <@SchemaPrefix/>EVENEMENT_CIVIL_ECH
  	where
      id in (${IDS})
      and etat = 'FORCE'
      and log_muser = '${USER}');

</#list>
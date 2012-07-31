<#macro StrOrNull value><#if value?trim = "">null<#else>'${value}'</#if></#macro>
<#macro NumOrNull value><#if value?trim = "">null<#else>${value}</#if></#macro>

<#--TODO Palliatif à valider pour la date de debut: 01.01.2000 si pas de valeur-->
<#macro DateOrDefault value><#if value?trim = "">20000101<#else>${value}</#if></#macro>

<#list tiers as t>
-----------------------------------
-- Migration du tiers ${t.NO_TIERS} (ancien individu ${t.NO_IND_REGPP})
-----------------------------------
update tiers set
	numero_individu = null,
	pp_habitant = 0,
	index_dirty = 1,
	date_deces = <@NumOrNull t.DATE_DECES />,
	nh_cat_etranger = <@StrOrNull t.CAT_ETRANGER />,
	nh_date_debut_valid_autoris = <@NumOrNull t.DATE_DEBUT_VALID_AUTORIS />,
	nh_date_naissance = <@NumOrNull t.DATE_NAISSANCE />,
	nh_no_ofs_nationalite =<@NumOrNull t.NO_OFS_NATIONALITE />,
	nh_nom = <@StrOrNull t.NOM />,
	nh_numero_assure_social =<@StrOrNull t.NO_AVS13 />,
	nh_prenom = <@StrOrNull t.PRENOM />,
	nh_sexe = <@StrOrNull t.SEXE />,
	log_muser = '${USER}',
	log_mdate = CURRENT_TIMESTAMP
where numero = ${t.NO_TIERS};

insert into remarque (
	id,        -- hibernate_sequence.nextval,
	log_cdate, -- CURRENT_TIMESTAMP
	log_cuser, -- '${USER}'
	log_mdate, -- CURRENT_TIMESTAMP
	log_muser, -- '${USER}'
	texte,     -- '${t.REMARQUE}'
	tiers_id   -- ${t.NO_TIERS}
)
select
	hibernate_sequence.nextval,
	CURRENT_TIMESTAMP,
	'${USER}',
	CURRENT_TIMESTAMP,
	'${USER}',
	'${t.REMARQUE}',
	${t.NO_TIERS}
from dual;

<#if t.ADRESSE_TYPE?trim != "">
insert into adresse_tiers (
	adr_type,                -- '${t.ADRESSE_TYPE}'
	id,                      -- hibernate_sequence.nextval
	permanente,              -- 0
	log_cdate,               -- CURRENT_TIMESTAMP
	log_cuser,               -- '${USER}'
	log_mdate,               -- CURRENT_TIMESTAMP
	log_muser,               -- '${USER}'
	date_debut,              -- <@DateOrDefault t.ADRESSE_DATE_DEBUT />
	usage_type,              -- 'COURRIER'
	numero_appartement,      -- <@StrOrNull t.ADRESSE_NO_APPARTEMENT />
	numero_case_postale,     -- <@NumOrNull t.ADRESSE_NO_CASE_POSTALE />
	rue,                     -- <@StrOrNull t.ADRESSE_RUE />
	texte_case_postale,      -- <@StrOrNull t.ADRESSE_TEXTE_CASE_POSTALE/>
	numero_ofs_pays,         -- <@NumOrNull t.ADRESSE_NO_OFS_PAYS />
	numero_postal_localite,  -- <@StrOrNull t.ADRESSE_NO_POSTAL />
	numero_ordre_poste,      -- <@NumOrNull t.ADRESSE_NO_ORDRE_POSTAL />
	numero_rue,              -- <@NumOrNull t.ADRESSE_NO_RUE/>
	npa_case_postale,        -- <@NumOrNull t.ADRESSE_NPA_CASE_POSTALE/>
	tiers_id                 -- ${t.NO_TIERS},
)
select
	'${t.ADRESSE_TYPE}',
	hibernate_sequence.nextval,
	0,
	CURRENT_TIMESTAMP,
	'${USER}',
	CURRENT_TIMESTAMP,
	'${USER}',
	<@DateOrDefault t.ADRESSE_DATE_DEBUT />, <#-- TODO Trouver un palliatif pour le cas ou la date debut, pour l'instant voir macro @DateOrDefault -->
	'COURRIER',
	<@StrOrNull t.ADRESSE_NO_APPARTEMENT />,
	<@NumOrNull t.ADRESSE_NO_CASE_POSTALE />,
	<@StrOrNull t.ADRESSE_RUE />,
	<@StrOrNull t.ADRESSE_TEXTE_CASE_POSTALE />,
	<@NumOrNull t.ADRESSE_NO_OFS_PAYS />,
	<@StrOrNull t.ADRESSE_NO_POSTAL />,
	<@NumOrNull t.ADRESSE_NO_ORDRE_POSTAL />,
	<@NumOrNull t.ADRESSE_NO_RUE/>,
	<@NumOrNull t.ADRESSE_NPA_CASE_POSTALE/>,
	${t.NO_TIERS}
from dual;
</#if>

<#if t.ETAT_CIVIL_TYPE?trim != "" >
insert into situation_famille (
	situation_famille_type, -- 'SituationFamille'
	id,                     -- hibernate_sequence.nextval
	log_cdate,              -- CURRENT_TIMESTAMP
	log_cuser,              -- '${USER}'
	log_mdate,              -- CURRENT_TIMESTAMP
	log_muser,              -- '${USER}'
	date_debut,             -- <@DateOrDefault t.ETAT_CIVIL_DATE_DEBUT />
	etat_civil,             -- '${t.ETAT_CIVIL_TYPE}'
	nombre_enfants,         -- 0
	ctb_id                  -- ${t.NO_TIERS}
)
select
	'SituationFamille',
	hibernate_sequence.nextval,
	CURRENT_TIMESTAMP,
	'${USER}',
	CURRENT_TIMESTAMP,
	'${USER}',
	<@DateOrDefault t.ETAT_CIVIL_DATE_DEBUT />, <#-- TODO Trouver un palliatif pour le cas ou la date debut, pour l'instant voir macro @DateOrDefault -->
	'${t.ETAT_CIVIL_TYPE}',
	0,
	${t.NO_TIERS}
from dual;
</#if>

<#if t.NO_AVS11?trim != "">
insert into identification_personne (
	id,					-- hibernate_sequence.nextval
	log_cdate,			-- CURRENT_TIMESTAMP
	log_cuser,			-- '${USER}'
	log_mdate,			-- CURRENT_TIMESTAMP
	log_muser,			-- '${USER}'
	categorie,			-- 'CH_AHV_AVS'
	identifiant,		-- '${t.NO_AVS11}'
	non_habitant_id		-- ${t.NO_TIERS}
)
select
	hibernate_sequence.nextval,
	CURRENT_TIMESTAMP,
	'${USER}',
	CURRENT_TIMESTAMP,
	'${USER}',
	'CH_AHV_AVS',
	'${t.NO_AVS11}',
	${t.NO_TIERS}
from dual;
</#if>

<#if t.NO_RCE?trim != "">
insert into identification_personne (
	id,					-- hibernate_sequence.nextval
	log_cdate,			-- CURRENT_TIMESTAMP
	log_cuser,			-- '${USER}'
	log_mdate,			-- CURRENT_TIMESTAMP
	log_muser,			-- '${USER}'
	categorie,			-- 'CH_ZAR_RCE'
	identifiant,		-- '${t.NO_RCE}'
	non_habitant_id		-- ${t.NO_TIERS}
)
select
	hibernate_sequence.nextval,
	CURRENT_TIMESTAMP,
	'${USER}',
	CURRENT_TIMESTAMP,
	'${USER}',
	'CH_ZAR_RCE',
	'${t.NO_RCE}',
	${t.NO_TIERS}
from dual;
</#if>

</#list>
<#--  le replace dans la macro StrOrNull sert à escaper les simple quotes dans les chaines de caratères SQL-->
<#macro StrOrNull value><#if value?trim = "">null<#else>'${value?replace("'", "''")}'</#if></#macro>
<#macro NumOrNull value><#if value?trim = "">null<#else>${value}</#if></#macro>
<#macro SexOrNull value><#if value?trim = "E1_masculin">'MASCULIN'<#elseif value?trim = "E2_feminin">'FEMININ'<#else>null</#if></#macro>
<#macro EqStrOrIsNull value><#if value?trim = "">is null<#else>= '${value?replace("'", "''")}'</#if></#macro>
<#macro EqNumOrIsNull value><#if value?trim = "">is null<#else>= ${value}</#if></#macro>
<#macro EqSexOrIsNull value><#if value?trim = "E1_masculin">= 'MASCULIN'<#elseif value?trim = "E2_feminin">= 'FEMININ'<#else>is null</#if></#macro>
<#macro SchemaPrefix>unireg.</#macro>
--------------------------------------------------
--           Rattrapage SQL ech99
--
-- Mise à jour des non-habitants impactés par un
-- evenement issu d'un ech99
--
-- Nombre total d'individu: ${TOTAL}
-- script généré le: ${DATE?datetime}
--------------------------------------------------

<#list LIST as INDIV>
update
	<@SchemaPrefix/>tiers
set
	nh_nom = <@StrOrNull INDIV.OfficialName/>,
	nh_prenom = <@StrOrNull INDIV.Call/>,
	nh_sexe = <@SexOrNull INDIV.Sex/>,
	nh_no_ofs_commune_origine = <@NumOrNull INDIV.OriginOfsId/>,
	log_mdate = CURRENT_TIMESTAMP,
	log_muser = '${USER}'
where
	numero_individu = ${INDIV.ID}
	and annulation_date is null
	and not exists (
		select
			numero from <@SchemaPrefix/>tiers
		where
			numero_individu = ${INDIV.ID}
			and nh_nom <@EqStrOrIsNull INDIV.OfficialName/>
			and nh_prenom <@EqStrOrIsNull INDIV.Call/>
			and nh_sexe <@EqSexOrIsNull INDIV.Sex/>
			and nh_no_ofs_commune_origine <@EqNumOrIsNull INDIV.OriginOfsId/>
			and annulation_date is null);

</#list>
<#macro StrOrNull value><#if value?trim = "">null<#else>'${value}'</#if></#macro>
<#macro NumOrNull value><#if value?trim = "">null<#else>${value}</#if></#macro>

<#list tiers as t>
update tiers set
		numero_individu = null,
		pp_habitant = 0,
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
		log_mdate = ${DATE}
where numero = ${t.NO_TIERS};

insert into remarque
  (id, log_cdate,log_cuser, texte, tiers_id)
values
  (hibernate_sequence.nextval, ${DATE}, '${USER}', '${t.REMARQUE}', ${t.NO_TIERS});

insert into adresse_tiers (
        adr_type,
		id,
		log_cdate,
		log_cuser,
		date_debut,
        usage_type,
		numero_appartement,
		numero_case_postale,
		rue,
        texte_case_postale,
		numero_ofs_pays,
		numero_postal_localite,
        numero_ordre_poste,
		numero_rue,
		tiers_id,
		npa_case_postale
) values (
        '${t.ADRESSE_TYPE}',
		hibernate_sequence.nextval,
		${DATE},
		'${USER}',
		${t.ADRESSE_DATE_DEBUT},
        'COURRIER',
		<@StrOrNull t.ADRESSE_NO_APPARTEMENT />,
		<@NumOrNull t.ADRESSE_NO_CASE_POSTALE />,
		<@NumOrNull t.ADRESSE_NO_RUE />,
		<@StrOrNull t.ADRESSE_TEXTE_CASE_POSTALE/>,
		<@NumOrNull NO_OFS_PAYS />,
		<@StrOrNull t.ADRESSE_NO_POSTAL />,
		<@NumOrNull t.ADRESSE_NO_ORDRE_POSTAL />,
		<@NumOrNull t.ADRESSE_NO_RUE/>,
		${t.NO_TIERS},
		<@NumOrNull t.ADRESSE_NPA_CASE_POSTALE/>);

</#list>
<%@ page language="java" contentType="text/html; charset=UTF-8"	pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Impossible de déterminer l'adresse d'un tiers</tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>

	<tiles:put name="body" type="String">
		<unireg:closeOverlayButton/>
		
		<p>Unireg n'est pas en mesure de construire l'adresse du tiers
		courant.</p>
		<p>Cette erreur peut survenir en cas des données incohérentes dans
		la base (par exemple si un tuteur a comme représentant le pupille dont
		il y a la charge: il y a une dépendence cyclique). Alternativement, il
		peut y avoir un problème dans l'application elle-même.</p>
		<h3>Essayer de corriger les adresses du tiers concerné.</h3>

		<br><hr><br>
		<unireg:callstack exception="${exception}"
			headerMessage="Si vous ne pouvez pas corriger les données vous-mêmes, veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous "></unireg:callstack>

	</tiles:put>
</tiles:insert>
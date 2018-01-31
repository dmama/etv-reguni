<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Problème de connexion au référentiel d'infrastructure</tiles:put>
	<tiles:put name="connected" type="String"></tiles:put>

	<tiles:put name="body" type="String">

		<p>Une erreur est survenue lors de la connexion au référentiel d'infrastructure.</p>
		<p>Le référentiel d'infrastructure semble être temporairement indisponible, ou un
		autre problème empêche le référentiel de fonctionner correctement.</p>
		<h3>Veuillez patienter un instant et réessayer.</h3>

		<br><hr><br>
		<unireg:callstack exception="${exception}"
			headerMessage="Si l'erreur persiste, veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous "></unireg:callstack>
	</tiles:put>
</tiles:insert>

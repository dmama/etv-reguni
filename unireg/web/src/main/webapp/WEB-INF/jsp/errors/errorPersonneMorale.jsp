<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Problème de connexion au registre civil</tiles:put>
	<tiles:put name="connected" type="String"></tiles:put>

	<tiles:put name="body" type="String">

		<p>Une erreur est survenue lors de la connexion au registre Personne morale.</p>
		<p>Le registre Personne morale peut être temporairement indisponible, ou un
		autre problème prévient le registre de fonctionner correctement.</p>
		<h3>Veuillez patientez un instant et réessayer.</h3>

		<br><hr><br>
		<unireg:callstack exception="${exception}"
			headerMessage="Si l'erreur persiste, veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous "></unireg:callstack>
	</tiles:put>
</tiles:insert>

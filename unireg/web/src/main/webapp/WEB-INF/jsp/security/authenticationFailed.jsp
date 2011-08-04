<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateLight.jsp">
	<tiles:put name="menu" type="String"></tiles:put>

	<tiles:put name="title" type="String">Authentification échouée</tiles:put>
	<tiles:put name="body" type="String">

		<p>Une erreur est survenue lors de l'ouverture de la session utilisateur.</p>
		<c:if test="${exception == null}" >
			<p>Il est probable que vous ne possédez aucun droit d'accès à cette application.</p>
			<h3>Veuillez patientez un instant et réessayer.</h3>
			<p>Si le problème persiste, veuillez contacter votre administrateur.</p>
		</c:if>
		<c:if test="${exception != null}" >
			<p>Le service de sécurité IfoSec peut être temporairement indisponible, ou un autre
			problème empêche IfoSec de fonctionner correctement. Il est aussi
			possible que vous ne possédez aucun droit d'accès à cette application.</p>
			<h3>Veuillez patientez un instant et réessayer.</h3>
			<p>Si le problème persiste, veuillez contacter votre administrateur.</p>
			<br>
			<hr>
			<br>
			<unireg:callstack exception="${exception}"
				headerMessage="Si l'erreur persiste, veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous "></unireg:callstack>
		</c:if>
	</tiles:put>
</tiles:insert>

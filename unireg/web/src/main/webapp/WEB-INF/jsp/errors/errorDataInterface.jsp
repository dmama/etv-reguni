<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="menu" type="String"></tiles:put>
	<tiles:put name="title" type="String">Problème de consistence des données du registre civil</tiles:put>
	
	<tiles:put name="body" type="String">

		<h2>Unireg a détecté un problème de consistence des données dans le registre civil.</h2>
		<hr>
		<p>Le problème détecté est le suivant:</p>
		<p><span style="margin: 2em; font-weight: bold;"><c:out value="${exception.message}" /></span></p>
		
		<p>Veuillez corriger le problème directement dans le registre civil, ou contacter l'administrateur en lui communiquant le message d'erreur ci-dessus. Merci.</p>
	</tiles:put>
</tiles:insert>

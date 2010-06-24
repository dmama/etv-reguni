<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Accès refusé</tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>
	<tiles:put name="body" type="String">
		<unireg:closeOverlayButton/>
		<p>Vous ne possédez pas les droits suffisants pour accéder à cette page (compte utilisateur <b><authz:authentication operation="username"/></b>).</p>
		<p>L'application a retourné l'erreur suivante:</p>
		<span style="margin-left: 2em; color: red;"><c:out value="${exception.message}" /></span>
	</tiles:put>
</tiles:insert>
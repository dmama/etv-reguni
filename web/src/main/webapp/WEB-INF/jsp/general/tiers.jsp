<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="page" value="${param.page}" />
<c:set var="path" value="${param.path}" />
<c:set var="idBandeau" value="${param.idBandeau}"/>
<c:set var="commandName">
	<c:choose>
		<c:when test="${param.commandName == null || param.commandName == ''}">command</c:when>
		<c:otherwise>${param.commandName}</c:otherwise>
	</c:choose>
</c:set>
<c:set var="bind" value="${commandName}.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>

<c:if test="${path != 'tiersLie'}">
	<c:set var="titre"><fmt:message key="caracteristiques.tiers"/></c:set>
</c:if>
<c:if test="${path == 'tiersLie'}">
	<c:set var="titre"><fmt:message key="caracteristiques.tiers.lie"/></c:set>
</c:if>

<c:set var="showLinks" value="${page == 'visu'}"/>

<!-- Debut Caracteristiques generales -->
<c:choose>
	<c:when test="${fn:length(idBandeau) > 0}">
		<unireg:bandeauTiers numero="${tiersGeneral.numero}" titre="${titre}" showValidation="true" showEvenementsCivils="true" showLinks="${showLinks}" urlRetour="${urlRetour}" id="${idBandeau}"/>
	</c:when>
	<c:otherwise>
		<unireg:bandeauTiers numero="${tiersGeneral.numero}" titre="${titre}" showValidation="true" showEvenementsCivils="true" showLinks="${showLinks}" urlRetour="${urlRetour}"/>
	</c:otherwise>
</c:choose>
<!-- Fin Caracteristiques generales -->

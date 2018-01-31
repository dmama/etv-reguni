<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="page" value="${param.page}" />
<c:set var="path" value="${param.path}" />
<c:set var="commandName" value="${param.commandName}" />
<c:if test="${empty commandName}">
	<c:set var="commandName" value="command" />
</c:if>
<c:set var="bind" value="${commandName}.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>

<c:set var="className" value="information" />
<c:if test="${not empty param.className}">
	<c:set var="className" value="${param.className}" />
</c:if>

<c:set var="titre"><fmt:message key="caracteristiques.contribuable"/></c:set>

<!-- Debut Caracteristiques generales -->
<unireg:bandeauTiers numero="${tiersGeneral.numero}" titre="${titre}" cssClass="${className}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false"/>
<!-- Fin Caracteristiques generales -->
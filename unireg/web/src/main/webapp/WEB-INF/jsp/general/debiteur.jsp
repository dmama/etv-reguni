<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="page" value="${param.page}" />
<c:set var="path" value="${param.path}" />

<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>

<c:set var="titre"><fmt:message key="caracteristiques.debiteur.is"/></c:set>

<!-- Debut Caracteristiques generales -->
<unireg:bandeauTiers numero="${tiersGeneral.numero}" titre="${titre}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>
<!-- Fin Caracteristiques generales -->
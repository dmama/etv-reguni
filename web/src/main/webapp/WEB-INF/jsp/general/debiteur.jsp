<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:set var="idDebiteur" value="${param.idDebiteur}" />
<c:set var="titre"><fmt:message key="caracteristiques.debiteur.is"/></c:set>

<!-- Debut Caracteristiques generales -->
<unireg:bandeauTiers numero="${idDebiteur}" titre="${titre}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>
<!-- Fin Caracteristiques generales -->
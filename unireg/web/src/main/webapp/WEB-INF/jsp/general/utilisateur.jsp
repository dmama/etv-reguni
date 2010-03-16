<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="titleKey" value="${param.titleKey}"/>
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="utilisateur" value="${status.value}"  scope="request"/>
</spring:bind>
<!-- Debut Caracteristiques generales -->
<fieldset class="information">
	<legend><span><fmt:message key="${titleKey}" /></span></legend>
	<table CELLSPACING=0 CELLPADDING=5>
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.visa.operateur" />&nbsp;:</td>
			<td width="75%">${utilisateur.visaOperateur}</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.numero.registre.habitant" />&nbsp;:</td>
			<td width="75%">${utilisateur.numeroIndividu}</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.prenom.nom" />&nbsp;:</td>
			<td width="75%">${utilisateur.prenomNom}</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.office.impot" />&nbsp;:</td>
			<td width="75%">${utilisateur.officeImpot}</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Caracteristiques generales -->
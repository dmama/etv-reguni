<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<fieldset>
	<legend><span><fmt:message key="label.entreprise" /></span></legend>
	<c:set var="ligneTableau" value="${1}" scope="request" />
	<table>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="50%"><fmt:message key="label.nom" />&nbsp;:</td>
			<td width="50%">${command.entreprise.raisonSociale}</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="50%"><fmt:message key="label.forme.juridique" />&nbsp;:</td>
			<td width="50%">${command.entreprise.formeJuridique}</td>
		</tr>
	</table>
</fieldset>


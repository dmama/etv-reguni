<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td width="25%"><fmt:message key="label.debiteur.is" />&nbsp;:</td>
	<td width="50%"><fmt:message key="option.categorie.impot.source.${tiersGeneral.categorie}" /></td>
	<td width="25%">&nbsp;</td>
</tr>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td width="25%"><fmt:message key="label.periodicite" />&nbsp;:</td>
	<td width="50%">
		<fmt:message key="option.periodicite.decompte.${tiersGeneral.periodicite}" />
		<c:if test="${tiersGeneral.periodicite == 'UNIQUE'}">
			&nbsp;(<fmt:message key="option.periode.decompte.${tiersGeneral.periode}" />)
		</c:if>
	</td>
	<td width="25%">&nbsp;</td>
</tr>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td width="25%"><fmt:message key="label.mode.communication" />&nbsp;:</td>
	<td width="50%"><fmt:message key="option.mode.communication.${tiersGeneral.modeCommunication}" /></td>
	<td width="25%">&nbsp;</td>
</tr>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td width="25%"><fmt:message key="label.personne.contact" />&nbsp;:</td>
	<td width="50%">${tiersGeneral.personneContact}</td>
	<td width="25%">&nbsp;</td>	
</tr>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td width="25%"><fmt:message key="label.numero.telephone.fixe" />&nbsp;:</td>
	<td width="50%">${tiersGeneral.numeroTelephone}</td>
	<td width="25%">&nbsp;</td>
</tr>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td width="25%"><fmt:message key="label.date.naissance" />&nbsp;:</td>
	<td width="50%">
		<unireg:regdate regdate="${tiersGeneral.dateNaissance}" />
	</td>
	<td width="25%">&nbsp;</td>
</tr>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td width="25%"><fmt:message key="label.nouveau.numero.avs" />&nbsp;:</td>
	<td width="50%">
		<unireg:numAVS  numeroAssureSocial="${tiersGeneral.numeroAssureSocial}" />
	</td>
	<td width="25%">&nbsp;</td>
</tr>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td width="25%"><fmt:message key="label.ancien.numero.avs" />&nbsp;:</td>
	<td width="50%">
		<unireg:ancienNumeroAVS ancienNumeroAVS="${tiersGeneral.ancienNumeroAVS}" />
	</td>
	<td width="25%">&nbsp;</td>
</tr>
<c:if test="${path == 'premierePersonne'}">
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td colspan="3">
			<form:errors path="premierePersonne" cssClass="error"/>
		</td>
	</tr>
</c:if>
<c:if test="${path == 'secondePersonne'}">
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td colspan="3">
			<form:errors path="secondePersonne" cssClass="error"/>
		</td>
	</tr>
</c:if>
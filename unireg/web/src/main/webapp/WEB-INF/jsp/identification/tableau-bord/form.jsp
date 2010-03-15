<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<b><fmt:message key="label.message" /></b>
		</td>
		<td width="25%">&nbsp;</td>
		<td width="25%">&nbsp;</td>
		<td width="25%">&nbsp;</td>
	</tr>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<fmt:message key="label.type.message" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="typeMessage">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${typesMessage}" />
			</form:select>	
		</td>
		<td width="25%">&nbsp;</td>
		<td width="25%">&nbsp;</td>
	</tr>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">
			<fmt:message key="label.periode.fiscale" />&nbsp;:
		</td>
		<td width="25%">
			<form:select path="periodeFiscale">
				<form:option value="-1" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${periodesFiscales}" />
			</form:select>
		</td>
		<td width="25%">&nbsp;</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Debut Boutons -->
<table border="0">
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.calculer"/>" name="calculer"/></div>
			
			
		</td>
		<td width="25%"><div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.effacer"/>" name="effacer" /></div></td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Fin Boutons -->

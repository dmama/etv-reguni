<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Listes recapitulatives -->
<fieldset class="information">
	<legend><span><fmt:message key="caracteristiques.lr" /></span></legend>
	
	<table>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.date.debut.periode" />&nbsp;:</td>
			<td width="25%"><fmt:formatDate value="${command.dateDebutPeriode}" pattern="dd.MM.yyyy"/></td>
			<td width="25%"><fmt:message key="label.date.fin.periode" />&nbsp;:</td>
			<td width="25%"><fmt:formatDate value="${command.dateFinPeriode}" pattern="dd.MM.yyyy"/></td>
		</tr>
		<c:if test="${command.id != null }">
			<c:if test="${command.dateRetour != null }">
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td width="25%"><fmt:message key="label.date.retour" />&nbsp;:</td>
				<td width="25%"><fmt:formatDate value="${command.dateRetour}" pattern="dd.MM.yyyy"/></td>
				<td width="25%">&nbsp;</td>
				<td width="25%">&nbsp;</td>
			</tr>
			</c:if>
		</c:if>
		<c:if test="${command.id == null }">
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.sans.sommation" />&nbsp;:</td>
			<td width="25%">
				<form:checkbox path="sansSommation" />
			</td>
			<td width="25%"><fmt:message key="label.date.delai.accorde" />&nbsp;:</td>
			<td width="25%">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="delaiAccorde" />
					<jsp:param name="id" value="delaiAccorde" />
				</jsp:include>
			</td>
		</tr>
		</c:if>
	</table>
	
</fieldset>
<!-- Fin  Listes recapitulatives -->
<!-- Debut Delais -->
<c:if test="${command.id != null }">
	<jsp:include page="delais.jsp"/>
</c:if>
<!-- Fin Delais -->
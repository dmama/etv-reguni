<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Listes recapitulatives -->
<fieldset class="information">
	<legend><span><fmt:message key="caracteristiques.lr" /></span></legend>
	
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.debut.periode" />&nbsp;:</td>
			<td width="25%"><fmt:formatDate value="${command.dateDebutPeriode}" pattern="dd.MM.yyyy"/></td>
			<td width="25%"><fmt:message key="label.date.fin.periode" />&nbsp;:</td>
			<td width="25%"><fmt:formatDate value="${command.dateFinPeriode}" pattern="dd.MM.yyyy"/></td>
		</tr>
		<c:if test="${command.id != null }">
			<c:if test="${command.dateRetour != null }">
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.date.retour" />&nbsp;:</td>
				<td width="25%"><fmt:formatDate value="${command.dateRetour}" pattern="dd.MM.yyyy"/></td>
				<td width="25%">&nbsp;</td>
				<td width="25%">&nbsp;</td>
			</tr>
			</c:if>
		</c:if>
		<c:if test="${command.id == null }">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.delai.accorde" />&nbsp;:</td>
			<td width="25%">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="delaiAccorde" />
					<jsp:param name="id" value="delaiAccorde" />
				</jsp:include>
			</td>
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
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
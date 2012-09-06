<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="depuisTache" value="${param.depuisTache}" />
<!-- Debut Declaration impot -->
<fieldset class="information">
	<legend><span><fmt:message key="label.caracteristiques.di" /></span></legend>

	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
			<td width="25%">${command.periodeFiscale}</td>
			<td width="25%"><fmt:message key="label.code.controle"/>&nbsp;:</td>
			<td width="25%">${command.codeControle}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.debut.periode.imposition" />&nbsp;:</td>
			<td width="25%">
				<fmt:formatDate value="${command.dateDebutPeriodeImposition}" pattern="dd.MM.yyyy"/>
			</td>
			<td width="25%"><fmt:message key="label.date.fin.periode.imposition" />&nbsp;:</td>
			<td width="25%">
				<fmt:formatDate value="${command.dateFinPeriodeImposition}" pattern="dd.MM.yyyy"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.type.declaration" />&nbsp;:</td>
			<td width="25%">
				<c:if test="${command.allowedQuittancement && command.typeDeclarationImpot.ordinaire}">
					<form:select path="typeDeclarationImpot" items="${typesDeclarationImpotOrdinaire}" />
				</c:if>
				<c:if test="${!command.allowedQuittancement || !command.typeDeclarationImpot.ordinaire}">
					<fmt:message key="option.type.document.${command.typeDeclarationImpot}" />
				</c:if>
			</td>
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.retour" />&nbsp;:</td>
			<td width="25%">
			<c:if test="${command.allowedQuittancement}">
				<c:if test="${depuisTache == null}">
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateRetour" />
						<jsp:param name="id" value="dateRetour" />
					</jsp:include>
				</c:if>
				<c:if test="${depuisTache != null}">
					<fmt:formatDate value="${command.dateRetour}" pattern="dd.MM.yyyy" />
				</c:if>
			</c:if>
			<c:if test="${!command.allowedQuittancement}">
				<fmt:formatDate value="${command.dateRetour}" pattern="dd.MM.yyyy"/>
			</c:if>
			</td>
			<td width="25%"><fmt:message key="label.source"/>&nbsp;:</td>
			<td width="25%">
				<c:if test="${command.dateRetour != null}">
					<c:if test="${command.sourceRetour == null}">
						<fmt:message key="option.source.quittancement.UNKNOWN" />
					</c:if>
					<c:if test="${command.sourceRetour != null}">
						<fmt:message key="option.source.quittancement.${command.sourceRetour}" />
					</c:if>
				</c:if>
			</td>
		</tr>

	</table>

</fieldset>
<!-- Fin  Declaration impot -->
<!-- Debut Delais -->
<jsp:include page="delais.jsp"/>
<!-- Fin Delais -->
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="depuisTache" value="${param.depuisTache}" />
<!-- Debut Declaration impot -->
<fieldset class="information">
	<FONT COLOR="#FF0000">${erreurCommunicationEditique}</FONT>
	<legend><span><fmt:message key="label.caracteristiques.di" /></span></legend>

	<c:if test="${command.allowedQuittancement && command.id == null && command.dateRetour != null && command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}">
		<div class="flash"><fmt:message key="label.di.proposition.retour.car.annulee.existe"/></div>
	</c:if>

	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
			<td width="25%">${command.periodeFiscale}</td>
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.debut.periode.imposition" />&nbsp;:</td>
			<td width="25%">
				<c:if test="${not command.ouverte}">
					<fmt:formatDate value="${command.dateDebutPeriodeImposition}" pattern="dd.MM.yyyy"/>
				</c:if>
				<c:if test="${command.ouverte}">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateDebutPeriodeImposition" />
							<jsp:param name="id" value="dateDebutPeriodeImposition" />
						</jsp:include>
				</c:if>				
			</td>
			<td width="25%"><fmt:message key="label.date.fin.periode.imposition" />&nbsp;:</td>
			<td width="25%">
				<c:if test="${not command.ouverte}">
					<fmt:formatDate value="${command.dateFinPeriodeImposition}" pattern="dd.MM.yyyy"/>
				</c:if>
				<c:if test="${command.ouverte}">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateFinPeriodeImposition" />
							<jsp:param name="id" value="dateFinPeriodeImposition" />
						</jsp:include>
				</c:if>
			</td>
		</tr>
		<c:if test="${command.id != null}">
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
				<c:if test="${command.allowedQuittancement}">
					<td width="25%">
						<c:if test="${depuisTache == null}">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateRetour" />
								<jsp:param name="id" value="dateRetour" />
							</jsp:include>
						</c:if>
						<c:if test="${depuisTache != null}">
							<fmt:formatDate value="${command.dateRetour}" pattern="dd.MM.yyyy" />
						</c:if>
					</td>

				</c:if>
				<c:if test="${!command.allowedQuittancement}">
					<td width="25%"><fmt:formatDate value="${command.dateRetour}" pattern="dd.MM.yyyy"/></td>
				</c:if>
				<td width="25%">&nbsp;</td>
				<td width="25%">&nbsp;</td>
			</tr>
		</c:if>
		<c:if test="${command.id == null}">
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.type.declaration" />&nbsp;:</td>
				<td width="25%">
					<form:select path="typeDeclarationImpot" items="${typesDeclarationImpot}" />
				</td>
				<td width="25%"><fmt:message key="label.date.delai.accorde" />&nbsp;:</td>
					<td width="25%">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="delaiAccorde" />
							<jsp:param name="id" value="delaiAccorde" />
						</jsp:include>
						<FONT COLOR="#FF0000">*</FONT>
					</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.type.adresse.retour" />&nbsp;:</td>
				<td width="25%">
					<form:select path="typeAdresseRetour" items="${typesAdresseRetour}" />
				</td>
                <c:if test="${command.allowedQuittancement && command.dateRetour != null && command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}">
					<td width="25%"><fmt:message key="label.date.retour" />&nbsp;:</td>
					<td width="25%">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateRetour" />
							<jsp:param name="id" value="dateRetour" />
							<jsp:param name="inputFieldClass" value="flash" />
						</jsp:include>
					</td>
                </c:if>
                <c:if test="${!command.allowedQuittancement || command.dateRetour == null || !command.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste}">
					<td width="25%">&nbsp;</td>
					<td width="25%">&nbsp;</td>
				</c:if>
			</tr>
		</c:if>
		
	</table>

</fieldset>
<!-- Fin  Declaration impot -->
<!-- Debut Delais -->
<c:if test="${command.id != null }">
	<jsp:include page="delais.jsp"/>
</c:if>
<!-- Fin Delais -->
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"/>
	<tiles:put name="title">
		<fmt:message key="title.ajout.etat.di">
			<fmt:param>${command.periodeFiscale}</fmt:param>
			<fmt:param><unireg:date date="${command.dateDebutPeriodeImposition}"/></fmt:param>
			<fmt:param><unireg:date date="${command.dateFinPeriodeImposition}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">

		<form:form method="post" name="theForm" id="formAddEtat" action="ajouter-quittance.do">

			<form:errors cssClass="error"/>

			<form:hidden path="id" value="${command.id}"/>

			<fieldset style="width:35em;">
				<legend><span><fmt:message key="label.etats"/></span></legend>
				<table border="0" style="padding: 0.5em;">
					<tr class="<unireg:nextRowClass/>">
						<td width="50%">
							<form:label path="typeDocument"><fmt:message key="label.type.declaration.retournee"/>&nbsp;:</form:label>
						</td>
						<td>
							<c:choose>
								<c:when test="${command.typeDocumentEditable}">
									<%--@elvariable id="typesDeclarationImpotOrdinaire" type="java.util.Map<TypeDocument, String>"--%>
									<form:select path="typeDocument" items="${typesDeclarationImpotOrdinaire}"/>
								</c:when>
								<c:otherwise>
									<form:hidden path="typeDocument"/>
									<c:if test="${command.typeDocument != null}">
										<fmt:message key="option.type.document.${command.typeDocument}"/>
									</c:if>
								</c:otherwise>
							</c:choose>
							<form:errors path="typeDocument" cssClass="error"/>
							<form:hidden path="typeDocumentEditable"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td>
							<form:label path="dateRetour"><fmt:message key="label.date.retour"/>&nbsp;:</form:label>
						</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateRetour"/>
								<jsp:param name="id" value="dateRetour"/>
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>
				</table>
			</fieldset>

			<table border="0" style="width:35em;">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%">
						<input type="submit" value="<fmt:message key="label.bouton.quittancer" />"/>
					</td>
					<td width="25%">
							<unireg:buttonTo id="annuler" name="Annuler" action="/di/editer.do" method="get" params="{id:${command.id}}" />
					<td width="25%">&nbsp;</td>
				</tr>
			</table>

		</form:form>

	</tiles:put>
</tiles:insert>

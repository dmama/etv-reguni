<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="ajouterQuittance" type="ch.vd.unireg.documentfiscal.AjouterQuittanceDocumentFiscalView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"/>
	<tiles:put name="title">
		<fmt:message key="title.ajout.etat.docfisc">
			<fmt:param>${ajouterQuittance.libelleTypeDocument}</fmt:param>
			<fmt:param><unireg:numCTB numero="${ajouterQuittance.tiersId}"/></fmt:param>
			<fmt:param>${ajouterQuittance.periodeFiscale}</fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">

		<%--@elvariable id="ajouterQuittance" type="ch.vd.unireg.documentfiscal.AjouterQuittanceDocumentFiscalView"--%>
		<form:form method="post" name="theForm" id="formAddEtat" action="ajouter-quittance.do" modelAttribute="ajouterQuittance">

			<form:errors cssClass="error"/>

			<form:hidden path="id" value="${ajouterQuittance.id}"/>

			<fieldset style="width:35em;">
				<legend><span><fmt:message key="label.etats"/></span></legend>
				<table border="0" style="padding: 0.5em;">
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
							<unireg:buttonTo id="annuler" name="Annuler" action="/autresdocs/editer.do" method="get" params="{id:${ajouterQuittance.id}}" />
					<td width="25%">&nbsp;</td>
				</tr>
			</table>

		</form:form>

	</tiles:put>
</tiles:insert>

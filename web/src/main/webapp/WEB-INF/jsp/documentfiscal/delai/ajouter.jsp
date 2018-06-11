<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="ajouterDelai" type="ch.vd.unireg.documentfiscal.AjouterDelaiDocumentFiscalView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"/>
	<tiles:put name="title">
		<c:set var="titleKey" value="title.enregistrement.demande.delai.docfisc"/>
		<fmt:message key="${titleKey}">
			<fmt:param>${ajouterDelai.periode}</fmt:param>
			<fmt:param><unireg:numCTB numero="${ajouterDelai.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<%--@elvariable id="ajouterDelai" type="ch.vd.unireg.documentfiscal.AjouterDelaiDocumentFiscalView"--%>
		<form:form method="post" name="theForm" id="formAddDelai" action="ajouter.do" modelAttribute="ajouterDelai">

			<form:errors cssClass="error"/>

			<form:hidden path="idDocumentFiscal"/>
			<form:hidden path="ancienDelaiAccorde"/>

			<fieldset>
				<legend><span><fmt:message key="label.etats"/></span></legend>
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 25%;"><fmt:message key="label.date.demande"/>&nbsp;:</td>
						<td style="width: 25%;">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDemande"/>
								<jsp:param name="id" value="dateDemande"/>
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
						<td style="width: 25%;"><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
						<td style="width: 25%;"><unireg:date date="${ajouterDelai.ancienDelaiAccorde}"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 25%;"></td>
						<td style="width: 25%;"></td>
						<td style="width: 25%;"><fmt:message key="label.date.delai.accorde"/>&nbsp;:</td>
						<td style="width: 25%;">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="delaiAccordeAu"/>
								<jsp:param name="id" value="delaiAccordeAu"/>
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>
				</table>

			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%">
						<input type="submit" id="ajouter" value="Ajouter">
					</td>
					<td width="25%">
						<input type="button" value="<fmt:message key="label.bouton.annuler"/>" onclick="Navigation.backTo(['/autresdocs/editer.do', '/qsnc/editer.do'], '/autresdocs/editer.do', 'id=${ajouterDelai.idDocumentFiscal}')" />
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

	</tiles:put>
</tiles:insert>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"/>
	<tiles:put name="title">
		<fmt:message key="title.ajout.etat.questionnaire.snc">
			<fmt:param>${quittance.periodeFiscale}</fmt:param>
			<fmt:param><unireg:numCTB numero="${quittance.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">

		<form:form method="post" name="theForm" id="formQuittance" action="ajouter-quittance.do" modelAttribute="quittance">

			<form:errors cssClass="error"/>

			<form:hidden path="questionnaireId"/>
			<form:hidden path="periodeFiscale"/>
			<form:hidden path="tiersId"/>
			<form:hidden path="dateEmission"/>

			<unireg:nextRowClass reset="1"/>
			<fieldset>
				<legend><span><fmt:message key="label.etats"/></span></legend>
				<table border="0" style="padding: 0.5em;">
					<tr class="<unireg:nextRowClass/>">
						<td width="20%">
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
						<unireg:buttonTo id="annuler" name="Annuler" action="/qsnc/editer.do" method="get" params="{id:${quittance.questionnaireId}}" />
					</td>
					<td width="25%">
						<input type="submit" value="<fmt:message key="label.bouton.quittancer" />"/>
					</td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>

		</form:form>

	</tiles:put>
</tiles:insert>

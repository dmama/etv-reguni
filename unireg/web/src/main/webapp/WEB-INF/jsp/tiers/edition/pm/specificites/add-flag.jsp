<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.ajout.specificite">
			<fmt:param>
				<unireg:numCTB numero="${command.pmId}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>

		<form:form id="addFlagForm" commandName="command" action="add.do?group=${group}">

			<fieldset>
				<legend><span><fmt:message key="label.specificite"/></span></legend>

				<form:hidden path="pmId"/>
				<unireg:nextRowClass reset="0"/>
				<table border="0">
					<tr class="<unireg:nextRowClass/>">
						<td width="25%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
						<td width="25%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut"/>
								<jsp:param name="id" value="dateDebut"/>
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
						<td width="25%"><fmt:message key="label.date.fin"/>&nbsp;:</td>
						<td width="25%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFin"/>
								<jsp:param name="id" value="dateFin"/>
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.type"/>&nbsp;</td>
						<td colspan="3">
							<form:select path="value">
							    <form:option value=""/>
								<form:options items="${flagTypes}"/>
							</form:select>
							<span style="color: red;">*</span>
							<form:errors path="value" cssClass="error"/>
						</td>
					</tr>
				</table>

			</fieldset>

			<!-- Debut Bouton -->
			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/flag-entreprise/edit-list.do" params="{pmId:${command.pmId},group:'${group}'}" method="GET"/></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
			<!-- Fin Bouton -->

		</form:form>

	</tiles:put>

</tiles:insert>
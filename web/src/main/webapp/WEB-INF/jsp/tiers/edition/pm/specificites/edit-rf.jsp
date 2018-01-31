<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.regime.fiscal">
			<fmt:param value="${command.portee}"/>
			<fmt:param>
				<unireg:numCTB numero="${command.pmId}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>

		<form:form id="editRegimeForm" commandName="command" action="edit.do">

			<fieldset>
				<legend><span><fmt:message key="label.regime.fiscal"/></span></legend>

				<form:hidden path="pmId"/>
				<form:hidden path="rfId"/>
				<form:hidden path="portee"/>
				<form:hidden path="dateDebut"/>
				<form:hidden path="dateFin"/>

				<unireg:nextRowClass reset="0"/>
				<table border="0">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
						<td width="25%"><unireg:regdate regdate="${command.dateDebut}"/></td>
						<td width="25%"><fmt:message key="label.date.fin"/>&nbsp;:</td>
						<td width="25%"><unireg:regdate regdate="${command.dateFin}"/>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.regime.fiscal.type" />&nbsp;:</td>
						<td>
							<form:select path="code">
								<form:options items="${typesRegimeFiscal}"/>
							</form:select>
							<span class="mandatory">*</span>
							<form:errors path="code" cssClass="error"/>
						</td>
						<td colspan="2">&nbsp;</td>
					</tr>
				</table>

			</fieldset>

			<!-- Debut Bouton -->
			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.sauver" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/regimefiscal/edit-list.do" params="{pmId:${command.pmId},portee:'${command.portee}'}" method="GET"/></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
			<!-- Fin Bouton -->

		</form:form>

	</tiles:put>

</tiles:insert>
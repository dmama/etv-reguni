<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.unireg.entreprise.FormeJuridiqueView.Edit"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.civil.forme.juridique">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false" titre="Caractéristiques de l'entreprise"/>

		<form:form id="editFormeJuridiqueForm" modelAttribute="command" action="edit.do">
			<fieldset>
				<legend><span><fmt:message key="label.forme.juridique" /></span></legend>

				<form:hidden path="id"/>
				<form:hidden path="tiersId"/>

				<!-- Debut FormeJuridique -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.forme.juridique"/>&nbsp;:</td>
						<td colspan="3">
							<form:select path="formeJuridique" name="formeJuridique">
								<form:option value="" />
								<form:options items="${formesJuridiquesEntrepriseEnum}"/>
							</form:select>
							<span class="mandatory">*</span>
							<form:errors path="formeJuridique" cssClass="error" />
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.date.ouverture" />&nbsp;:</td>
						<td width="30%">
							<form:hidden path="dateDebut"/>
							<unireg:regdate regdate="${command.dateDebut}"/>
						</td>
						<td width="20%"><fmt:message key="label.date.fermeture" />&nbsp;:</td>
						<td width="30%">
							<form:hidden path="dateFin"/>
							<unireg:regdate regdate="${command.dateFin}"/>
						</td>
					</tr>
				</table>
			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/civil/entreprise/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

	</tiles:put>
</tiles:insert>

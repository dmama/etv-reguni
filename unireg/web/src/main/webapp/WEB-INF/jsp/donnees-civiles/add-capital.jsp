<%@ page import="ch.vd.uniregctb.common.LengthConstants" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.CapitalView.Add"--%>

<c:set var="lengthmonnaie" value="<%=LengthConstants.MONNAIE_ISO%>" scope="request" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.civil.capital">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false" titre="Caractéristiques de l'entreprise"/>

		<form:form id="addCapitalForm" commandName="command" action="add.do">
			<fieldset>
				<legend><span><fmt:message key="label.capital" /></span></legend>

				<form:hidden path="tiersId"/>

				<!-- Debut Capital -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.capital"/>&nbsp;:</td>
						<td width="30%">
							<form:input path="montant" size="25"/>
							<span style="color: red;">*</span>
							<form:errors path="montant" cssClass="error" />
						</td>
						<td width="20%"><fmt:message key="label.capital.monnaie"/>&nbsp;:</td>
						<td width="30%">
							<form:input path="monnaie" size="3" maxlength="${lengthmonnaie}"/>
							<span style="color: red;">*</span>
							<form:errors path="monnaie" cssClass="error" />
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
							</jsp:include>
							<span style="color: red;">*</span>
						</td>
						<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFin" />
								<jsp:param name="id" value="dateFin" />
							</jsp:include>
						</td>
					</tr>
				</table>
			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/civil/entreprise/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

	</tiles:put>
</tiles:insert>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.AddRaisonSocialeView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.civil.raison.sociale">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="head">
		<style type="text/css">
			h1 {
				margin-left: 10px;
				padding-left: 40px;
				padding-top: 4px;
				height: 32px;
				background: url(../../css/x/fors/principal_32.png) no-repeat;
			}
		</style>
	</tiles:put>
	<tiles:put name="body">

		<form:form id="addRaisonSocialeForm" commandName="command" action="add.do">
			<fieldset>
				<legend><span><fmt:message key="label.raison.sociale" /></span></legend>

				<form:hidden path="tiersId"/>

				<!-- Debut RaisonSociale -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.raison.sociale"/>&nbsp;:</td>
						<td>
							<input id="raisonSociale" name="raisonSociale" size="25" />
							<form:errors path="raisonSociale" cssClass="error" />
						</td>
						<td width="20%"></td>
						<td>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
							</jsp:include>
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
					<td width="25%"><unireg:buttonTo name="Retour" action="/entreprise/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

	</tiles:put>
</tiles:insert>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.scission.entreprise">
			<fmt:param>
				<unireg:numCTB numero="${command.idEntrepriseScindee}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<c:set var="titre">
			<fmt:message key="label.caracteristiques.scission.entreprise.scindee"/>
		</c:set>
		<unireg:bandeauTiers numero="${command.idEntrepriseScindee}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true" titre="${titre}"/>
		<unireg:nextRowClass reset="0"/>

		<form:form method="post" id="recapDateScission" name="recapDateScission" action="choix-date.do">
			<form:hidden path="idEntrepriseScindee"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.scission.entreprise" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.contrat.scission" />&nbsp;:</td>
						<td width="75%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateContratScission" />
								<jsp:param name="id" value="dateContratScission" />
							</jsp:include>
							<span style="color: red;">*</span>
						</td>
					</tr>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="scindee/list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.selection.entreprises.resultantes"/>"/>
			<!-- Fin Boutons -->

		</form:form>
	</tiles:put>

</tiles:insert>
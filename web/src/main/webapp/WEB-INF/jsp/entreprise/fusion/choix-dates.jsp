<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="actionCommand" type="ch.vd.unireg.entreprise.complexe.FusionEntreprisesView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.fusion.entreprises"/>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${actionCommand.idEntrepriseAbsorbante}" titre="label.caracteristiques.fusion.entreprise.absorbante" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>
		<unireg:nextRowClass reset="0"/>

		<form:form method="post" id="recapDatesFusion" name="recapDatesFusion" action="choix-dates.do" commandName="actionCommand">
			<form:hidden path="idEntrepriseAbsorbante"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.fusion.entreprises" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.bilan.fusion" />&nbsp;:</td>
						<td width="75%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateBilanFusion" />
								<jsp:param name="id" value="dateBilanFusion" />
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.contrat.fusion" />&nbsp;:</td>
						<td width="75%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateContratFusion" />
								<jsp:param name="id" value="dateContratFusion" />
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="absorbante/list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.selection.entreprises.absorbees"/>"/>
			<!-- Fin Boutons -->

		</form:form>
	</tiles:put>

</tiles:insert>
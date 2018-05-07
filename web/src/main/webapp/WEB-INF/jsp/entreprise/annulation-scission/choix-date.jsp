<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="actionCommand" type="ch.vd.unireg.entreprise.complexe.ScissionEntrepriseView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.annulation.scission.entreprise">
			<fmt:param>
				<unireg:numCTB numero="${actionCommand.idEntrepriseScindee}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${actionCommand.idEntrepriseScindee}" titre="label.caracteristiques.scission.entreprise.scindee" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>
		<unireg:nextRowClass reset="0"/>

		<form:form method="post" id="recapDateScission" name="recapDateScission" action="choix-date.do" commandName="actionCommand">
			<form:hidden path="idEntrepriseScindee"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.scission.entreprise"/></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.contrat.scission" />&nbsp;:</td>
						<td width="75%">
							<%--@elvariable id="datesContrat" type="java.util.Map<String, String>"--%>
							<form:select path="dateContratScission" items="${datesContrat}"/>
							<span class="mandatory">*</span>
						</td>
					</tr>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment annuler la scission d\'entreprise ?');" />
			<!-- Fin Boutons -->

		</form:form>
	</tiles:put>

</tiles:insert>
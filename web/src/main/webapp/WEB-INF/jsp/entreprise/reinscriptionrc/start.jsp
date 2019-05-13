<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="actionCommand" type="ch.vd.unireg.entreprise.complexe.ReinscriptionRCView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.reinscription.rc.entreprise">
			<fmt:param>
				<unireg:numCTB numero="${actionCommand.idEntreprise}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${actionCommand.idEntreprise}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>
		<unireg:nextRowClass reset="0"/>

		<form:form method="post" id="recapReinscriptionRC" name="recapReinscriptionRC" modelAttribute="actionCommand">
			<form:hidden path="idEntreprise"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.faillite" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.radiation.rc" />&nbsp;:</td>
						<td width="75%">
							<form:hidden path="dateRadiationRC"/>
							<unireg:regdate regdate="${actionCommand.dateRadiationRC}"/>
							<form:errors path="dateRadiationRC"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td width="25%"><fmt:message key="label.commentaire" />&nbsp;:</td>
						<td width="75%">
							<form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
						</td>
					</tr>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment traiter la ré-inscription de cette entreprise au RC ?');" />
			<!-- Fin Boutons -->

		</form:form>
	</tiles:put>

</tiles:insert>
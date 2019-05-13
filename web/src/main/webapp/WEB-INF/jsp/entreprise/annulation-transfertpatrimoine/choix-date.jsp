<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="actionCommand" type="ch.vd.unireg.entreprise.complexe.TransfertPatrimoineView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.annulation.transfert.patrimoine">
			<fmt:param>
				<unireg:numCTB numero="${actionCommand.idEntrepriseEmettrice}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${actionCommand.idEntrepriseEmettrice}" titre="label.caracteristiques.transfert.patrimoine.emettrice" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>
		<unireg:nextRowClass reset="0"/>

		<form:form method="post" id="recapDateTransfert" name="recapDateTransfert" action="choix-date.do" modelAttribute="actionCommand">
			<form:hidden path="idEntrepriseEmettrice"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.transfert.patrimoine"/></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.transfert.patrimoine" />&nbsp;:</td>
						<td width="75%">
							<%--@elvariable id="datesTransfert" type="java.util.Map<String, String>"--%>
							<form:select path="dateTransfert" items="${datesTransfert}"/>
							<span class="mandatory">*</span>
						</td>
					</tr>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment annuler le transfert de patrimoine ?');" />
			<!-- Fin Boutons -->

		</form:form>
	</tiles:put>

</tiles:insert>
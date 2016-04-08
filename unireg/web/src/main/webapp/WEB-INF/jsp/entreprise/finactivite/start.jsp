<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.fin.activite.entreprise">
			<fmt:param>
				<unireg:numCTB numero="${command.idEntreprise}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.idEntreprise}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>

		<form:form method="post" id="recapFinActivite" name="recapFinActivite">
			<form:hidden path="idEntreprise"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.fin.activite" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.fin.activite" />&nbsp;:</td>
						<td width="75%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFinActivite" />
								<jsp:param name="id" value="dateFinActivite" />
							</jsp:include>
							<FONT COLOR="#FF0000">*</FONT>
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
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment traiter la fin d\'activité de cette entreprise ?');" />
			<!-- Fin Boutons -->

		</form:form>
	</tiles:put>

</tiles:insert>
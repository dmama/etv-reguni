<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.unireg.entreprise.EditRaisonEnseigneEtablissementView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.raison.enseigne">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false" titre="Caractéristiques de l'établissement"/>

		<form:form id="editRaisonEnseigneForm" commandName="command" action="edit.do">
			<fieldset>
				<legend><span><fmt:message key="label.raison.enseigne" /></span></legend>

				<form:hidden path="tiersId"/>

				<!-- Debut Capital -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.raison.sociale"/>&nbsp;:</td>
						<td>
							<form:input path="raisonSociale" size="80" />
							<span class="mandatory">*</span>
							<form:errors path="raisonSociale" cssClass="error" />
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.enseigne" />&nbsp;:</td>
						<td>
							<form:input path="enseigne" size="80" />
							<form:errors path="enseigne" cssClass="error" />
						</td>
					</tr>
				</table>
			</fieldset>

			<c:set var="confirmationMessageSauvegarde">
				<fmt:message key="label.demande.confirmation.sauvegarde"/>
			</c:set>
			<script type="text/javascript">
				var editCivilEtablissementNoms = {
					onSave : function(myform) {
						if (confirm('${confirmationMessageSauvegarde}')) {
							myform.submit();
						}
					}
				}
			</script>

			<c:set var="libelleBoutonRetour">
				<fmt:message key="label.bouton.retour"/>
			</c:set>
			<c:set var="confirmationMessageRetour">
				<fmt:message key="message.confirm.quit"/>
			</c:set>
			<unireg:buttonTo method="get" action="/civil/etablissement/edit.do" params="{id:${command.tiersId}}" name="${libelleBoutonRetour}" confirm="${confirmationMessageRetour}"/>
			<input type="button" name="save" value="<fmt:message key='label.bouton.sauver'/>" onclick="editCivilEtablissementNoms.onSave($('#editRaisonEnseigneForm'))"/>

		</form:form>

	</tiles:put>
</tiles:insert>

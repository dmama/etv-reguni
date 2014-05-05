<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ page import="ch.vd.uniregctb.common.LengthConstants" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.civil" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-civil-complement.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">

		<unireg:setAuth var="autorisations" tiersId="${tiersId}"/>
		<c:if test="${autorisations.donneesCiviles}">

			<unireg:bandeauTiers numero="${tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false"/>

			<form:form method="post" action="edit.do?id=${tiersId}" name="editCivilAutreCommunaute" commandName="data" id="editForm">
				<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
				<fieldset class="information">
					<legend><span><fmt:message key="label.organisation" /></span></legend>
					<unireg:nextRowClass reset="1"/>
					<table>
						<c:set var="length_ide" value="<%=LengthConstants.IDENT_ENTREPRISE_IDE + 3%>" scope="request" />
						<tr class="<unireg:nextRowClass/>" >
							<td width="30%"><fmt:message key="label.numero.ide"/>&nbsp;:</td>
							<td>
								<form:input path="ide" id="ac.ide" cssErrorClass="input-with-errors" size="20" maxlength="${length_ide}" tabindex="1"/>
								<form:errors path="ide" cssClass="error"/>
							</td>
						</tr>

						<c:set var="length_nom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.nom" />&nbsp;:</td>
							<td>
								<form:input path="nom" id="ac.nom" cssErrorClass="input-with-errors" size="65" maxlength="${length_nom}" tabindex="2"/>
								<FONT COLOR="#FF0000">*</FONT>
								<form:errors path="nom" cssClass="error"/>
							</td>
						</tr>

						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.forme.juridique" />&nbsp;:</td>
							<td>
								<form:select path="formeJuridique" items="${formesJuridiques}" tabindex="3"/>
							</td>
						</tr>
					</table>
				</fieldset>

				<c:set var="libelleBoutonRetour">
					<fmt:message key="label.bouton.retour"/>
				</c:set>
				<c:set var="confirmationMessageRetour">
					<fmt:message key="message.confirm.quit"/>
				</c:set>
				<unireg:buttonTo method="get" action="/tiers/visu.do" params="{id:${tiersId}}" name="${libelleBoutonRetour}" confirm="${confirmationMessageRetour}"/>
				<input type="button" name="save" value="<fmt:message key="label.bouton.sauver"/>" onclick="editCivilOrganisation.onSave($('#editForm'))"/>

				<c:set var="confirmationMessageSauvegarde">
					<fmt:message key="label.demande.confirmation.sauvegarde"/>
				</c:set>
				<script type="text/javascript">
					var editCivilOrganisation = {
						onSave : function(myform) {
							if (confirm('${confirmationMessageSauvegarde}')) {
								myform.submit();
							}
						}
					}
				</script>

			</form:form>

		</c:if>
		<c:if test="${!autorisations.donneesCiviles}">
			<span class="error"><fmt:message key="error.tiers.interdit" /></span>
		</c:if>

	</tiles:put>
</tiles:insert>

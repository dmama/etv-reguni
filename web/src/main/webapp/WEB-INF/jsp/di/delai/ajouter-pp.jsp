<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.unireg.di.view.AjouterDelaiDeclarationPPView"--%>
<%--@elvariable id="decisionsDelai" type="java.util.Map"--%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"/>
	<tiles:put name="title">
		<fmt:message key="title.ajout.delai.di">
			<fmt:param>${command.declarationPeriode}</fmt:param>
			<fmt:param><unireg:date date="${command.declarationRange.dateDebut}"/></fmt:param>
			<fmt:param><unireg:date date="${command.declarationRange.dateFin}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<form:form method="post" name="theForm" id="formAddDelai" action="ajouter-pp.do">

			<form:errors cssClass="error"/>

			<form:hidden path="idDeclaration"/>
			<form:hidden path="ancienDelaiAccorde"/>
			<form:hidden path="typeImpression" id="typeImpression"/>

			<fieldset>
				<legend><span><fmt:message key="label.etats"/></span></legend>
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 25%;"><fmt:message key="label.date.demande"/>&nbsp;:</td>
						<td style="width: 25%;">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDemande"/>
								<jsp:param name="id" value="dateDemande"/>
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
						<td style="width: 25%;"><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
						<td style="width: 25%;"><unireg:date date="${command.ancienDelaiAccorde}"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.decision"/>&nbsp;:</td>
						<td>
							<form:select id="decision" path="decision" onchange="refreshButtons();">
								<form:options items="${decisionsDelai}"/>
							</form:select>
						</td>
						<td>
							<div class="siDelaiAccorde"><fmt:message key="label.date.delai.accorde"/>&nbsp;:</div>
						</td>
						<td>
							<div class="siDelaiAccorde">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="delaiAccordeAu"/>
									<jsp:param name="id" value="delaiAccordeAu"/>
									<jsp:param name="mandatory" value="true" />
								</jsp:include>
							</div>
						</td>
					</tr>
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.confirmation.ecrite"/>&nbsp;:</td>
						<td>
							<form:checkbox path="confirmationEcrite" id="confirmation" onchange="refreshButtons();" onclick="refreshButtons();"/>
						</td>
						<td>&nbsp;</td>
						<td>&nbsp;</td>
					</tr>
				</table>

			</fieldset>

			<table border="0">
				<tr>
					<td width="25%" align="right">
						<input type="button" id="envoi-auto" value="Envoi courrier automatique" onclick="return ajouterDelai(this, 'BATCH');" style="display: none;">
					</td>
					<td width="25%">
						<input type="button" id="envoi-manuel" value="Envoi courrier manuel" onclick="return ajouterDelai(this, 'LOCAL');" style="display: none;">
						<input type="button" id="ajouter" value="Ajouter" onclick="return ajouterDelai(this, null);">
						<unireg:buttonTo id="retour" name="Retour" visible="false" action="/di/editer.do" method="get" params="{id:${command.idDeclaration}}"/>
					</td>
					<td width="25%">
						<unireg:buttonTo id="annuler" name="Annuler" action="/di/editer.do" method="get" params="{id:${command.idDeclaration}}"/>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

		<script type="text/javascript">

			function verifierDelaiDI() {
				let dateExpedition = '${command.dateExpedition}';
				let delaiAccordeAu = $('#delaiAccordeAu').val();
				let decisionSelectionnee = $('#decision').val();
				if (decisionSelectionnee === 'ACCORDE' && DateUtils.validate(delaiAccordeAu) && DateUtils.compare(DateUtils.addYear(dateExpedition, 1, 'yyyy.MM.dd'), DateUtils.getDate(delaiAccordeAu, 'dd.MM.yyyy')) === -1) {
					return confirm("Ce délai est située plus d'un an dans le futur à compter de la date d'expédition de la DI. Voulez-vous le sauver ?");
				}
				return true;
			}

			function ajouterDelai(button, type) {

				if (!verifierDelaiDI()) {
					return false;
				}

				$('#typeImpression').val(type);
				$('.error').hide();
				$(button).closest("form").submit();

				// On désactive les boutons
				$('#ajouter, #annuler, #envoi-auto, #envoi-manuel').hide();
				$('#retour').show();
				$('#confirmation').attr("disabled", true);

				return true;
			}

			function refreshButtons() {

				let confirmationInput = $('#confirmation');
				let confirmationEcrite = confirmationInput.attr('checked');
				let decisionSelectionnee = $('#decision').val();

				if (decisionSelectionnee === 'ACCORDE') {
					$('.siDelaiAccorde').show();
					if (confirmationEcrite) {
						$('#envoi-auto, #envoi-manuel').show();
						$('#ajouter').hide();
					}
					else {
						$('#envoi-auto, #envoi-manuel').hide();
						$('#ajouter').show();
					}
					confirmationInput.attr("disabled", false);
				}
				else if (decisionSelectionnee === 'REFUSE') {
					// on ne peut pas mettre de date de délai accordé sur un délai refusé
					$('.siDelaiAccorde').hide();
					if (confirmationEcrite) {
						$('#envoi-auto, #envoi-manuel').show();
						$('#ajouter').hide();
					}
					else {
						$('#envoi-auto, #envoi-manuel').hide();
						$('#ajouter').show();
					}
					confirmationInput.attr("disabled", false);
				}
				else {
					// on ne peut pas mettre de date de délai accordé sur un délai en attente
					$('.siDelaiAccorde').hide();
					$('#envoi-auto, #envoi-manuel').hide();
					$('#ajouter').show();
					// on ne peut pas demander une confirmation écrite sur un délai en attente
					confirmationInput.attr('checked', false);
					confirmationInput.attr("disabled", true);
				}
			}

			// première exécution au chargement de la page...
			refreshButtons();

		</script>
	</tiles:put>
</tiles:insert>

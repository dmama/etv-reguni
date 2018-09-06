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
							<form:select id="decision" path="decision" onchange="toggleDecision();">
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
							<form:checkbox path="confirmationEcrite" id="confirmation" onchange="toggleSubmitName();" onclick="toggleSubmitName();"/>
						</td>
						<td>&nbsp;</td>
						<td>&nbsp;</td>
					</tr>
				</table>

			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%">
						<input type="button" id="ajouter" value="Ajouter" onclick="return ajouterDelai(this);">
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
				var dateExpedition = '${command.dateExpedition}';
				var delaiAccordeAu = $('#delaiAccordeAu').val();
				if (DateUtils.validate(delaiAccordeAu) && DateUtils.compare(DateUtils.addYear(dateExpedition, 1, 'yyyy.MM.dd'), DateUtils.getDate(delaiAccordeAu, 'dd.MM.yyyy')) == -1) {
					return confirm("Ce délai est située plus d'un an dans le futur à compter de la date d'expédition de la DI. Voulez-vous le sauver ?");
				}
				return true;
			}

			function ajouterDelai(button) {

				if (!verifierDelaiDI()) {
					return false;
				}

				$('.error').hide();
				$(button).closest("form").submit();

				// On desactive les boutons
				$('#ajouter, #annuler').hide();
				$('#retour').show();
				$('#confirmation').attr("disabled", true);

				return true;
			}

			function toggleSubmitName() {
				if ($('#confirmation').attr('checked') && $('#decision').val() !== 'DEMANDE') {
					$('#ajouter').val('Imprimer');
				}
				else {
					$('#ajouter').val('Ajouter');
				}
			}

			function toggleDecision() {
				let isDelaiAccorde = $('#decision').val() === 'ACCORDE';

				// on ne peut pas demander une confirmation écrite que sur un délai accordé
				let confirmationInput = $('#confirmation');
				confirmationInput.attr("disabled", !isDelaiAccorde);
				if (!isDelaiAccorde) {
					confirmationInput.attr('checked', false)
				}

				// on peut pas mettre de date de délai accordé sur un délai non-accordé
				if (isDelaiAccorde) {
					$('.siDelaiAccorde').show();
				}
				else {
					$('.siDelaiAccorde').hide();
				}

				toggleSubmitName();
			}

			// première exécution au chargement de la page...
			toggleDecision();

		</script>
	</tiles:put>
</tiles:insert>

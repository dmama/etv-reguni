<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"/>
	<tiles:put name="title">
		<fmt:message key="title.modification.demande.delai.di">
			<fmt:param>${command.declarationPeriode}</fmt:param>
			<fmt:param><unireg:date date="${command.declarationRange.dateDebut}"/></fmt:param>
			<fmt:param><unireg:date date="${command.declarationRange.dateFin}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<form:form method="post" name="theForm" id="formModifDelai" action="editer-pm.do">

			<form:errors cssClass="error"/>

			<form:hidden path="idDelai"/>
			<form:hidden path="ancienDelaiAccorde"/>
			<form:hidden path="typeImpression" id="typeImpression"/>
			<form:hidden path="dateDemande"/>

			<fieldset>
				<legend><span><fmt:message key="label.etats"/></span></legend>
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 25%;"><fmt:message key="label.date.demande"/>&nbsp;:</td>
						<td style="width: 25%;"><unireg:date date="${command.dateDemande}"/></td>
						<td style="width: 25%;"><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
						<td style="width: 25%;"><unireg:date date="${command.ancienDelaiAccorde}"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.decision"/>&nbsp;:</td>
						<td>
							<form:select id="decision" path="decision" onchange="DelaiPM.toggleDecision();">
								<form:options items="${decisionsDelai}"/>
							</form:select>
						</td>
						<td>
							<div class="siDelaiAccorde">
								<fmt:message key="label.date.delai.accorde"/>&nbsp;:
							</div>
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
				</table>

			</fieldset>

			<table border="0">
				<tr>
					<td width="25%" align="right">
						<input type="button" id="envoi-auto" value="Envoi courrier automatique" onclick="return DelaiPM.modifierDelai(this, 'BATCH');" style="display: none;">
					</td>
					<td width="25%">
						<input type="button" id="envoi-manuel" value="Envoi courrier manuel" onclick="return DelaiPM.modifierDelai(this, 'LOCAL');" style="display: none;">
						<unireg:buttonTo id="retour" name="Retour" visible="false" action="/di/editer.do" method="get" params="{id:${command.idDeclaration}}"/>
					</td>
					<td width="25%">
						<unireg:buttonTo id="annuler" name="Annuler" action="/di/editer.do" method="get" params="{id:${command.idDeclaration}}"/>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

		<script type="text/javascript">

			var DelaiPM = {

				verifierDelaiDI: function() {
					var dateExpedition = '${command.dateExpedition}';
					var delaiAccordeAu = $('#delaiAccordeAu').val();
					if (DateUtils.validate(delaiAccordeAu) && DateUtils.compare(DateUtils.addYear(dateExpedition, 1, 'yyyy.MM.dd'), DateUtils.getDate(delaiAccordeAu, 'dd.MM.yyyy')) == -1) {
						return confirm("Ce délai est située plus d'un an dans le futur à compter de la date d'expédition de la DI. Voulez-vous le sauver ?");
					}
					return true;
				},

				modifierDelai: function(button, type) {
					if ($('#decision').val() == 'ACCORDE' && !this.verifierDelaiDI()) {
						return false;
					}

					$('#typeImpression').val(type);
					$(button).closest("form").submit();

					// On desactive les boutons
					$('#annuler, #envoi-auto, #envoi-manuel').hide();
					$('#retour').show();

					return true;
				},

				toggleDecision: function() {
					var decisionSelectionnee = $('#decision').val();
					$('.siDelaiAccorde, .siDelaiRefuse, #envoi-auto, #envoi-manuel').hide();
					if (decisionSelectionnee == 'ACCORDE') {
						$('.siDelaiAccorde').show();
						$('#envoi-auto, #envoi-manuel').show();
					}
					else if (decisionSelectionnee == 'REFUSE') {
						$('.siDelaiRefuse').show();
						$('#envoi-auto, #envoi-manuel').show();
					}
				}
			};

			// première exécution au chargement de la page...
			DelaiPM.toggleDecision();

		</script>
	</tiles:put>
</tiles:insert>

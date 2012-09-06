<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

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
		<form:form method="post" name="theForm" id="formAddDelai" action="ajouter.do">
			<form:hidden path="idDeclaration" value="${command.idDeclaration}"/>
			<fieldset>
				<legend><span><fmt:message key="label.delais"/></span></legend>
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>">
						<td/>
						<td/>
						<td><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
						<td><unireg:date date="${command.ancienDelaiAccorde}"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.date.demande"/>&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDemande"/>
								<jsp:param name="id" value="dateDemande"/>
							</jsp:include>
							<FONT COLOR="#FF0000">*</FONT>
						</td>
						<td><fmt:message key="label.date.delai.accorde"/>&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="delaiAccordeAu"/>
								<jsp:param name="id" value="delaiAccordeAu"/>
							</jsp:include>
							<FONT COLOR="#FF0000">*</FONT>
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
						<input type="button" id="retour" value="Retour" style="display:none;" onclick="document.location.href='../editer.do?id=' + ${command.idDeclaration}">
					</td>
					<td width="25%">
						<input type="button" id="annuler" value="Annuler" onclick="document.location.href='../editer.do?id=' + ${command.idDeclaration}">
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

		<script type="text/javascript">

			function verifierDelaiDI() {
				var dateExpedition = '${command.dateExpedition}';
				var delaiAccordeAu = $('#delaiAccordeAu').val();
				if (DateUtils.compare(DateUtils.addYear(dateExpedition, 1, 'yyyy.MM.dd'), DateUtils.getDate(delaiAccordeAu, 'dd.MM.yyyy')) == -1) {
					return confirm("Ce délai est située plus d'un an dans le futur à compter de la date d'expédition de la DI. Voulez-vous le sauver ?");
				}
				return true;
			}

			function ajouterDelai(button) {

				if (!verifierDelaiDI()) {
					return false;
				}

				$(button).closest("form").submit();

				if ($('#confirmation').attr('checked')) {
					// On desactive les boutons
					$('#ajouter').hide();
					$('#annuler').hide();
					$('#retour').show();
					$('#confirmation').attr("disabled", true);
				}

				return true;
			}

			function toggleSubmitName() {
				if ($('#confirmation').attr('checked')) {
					$('#ajouter').val('Imprimer');
				}
				else {
					$('#ajouter').val('Ajouter');
				}
			}

			// première exécution au chargement de la page...
			toggleSubmitName();

		</script>
	</tiles:put>
</tiles:insert>

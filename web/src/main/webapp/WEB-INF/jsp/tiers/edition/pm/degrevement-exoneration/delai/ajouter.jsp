<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="ajouterView" type="ch.vd.unireg.documentfiscal.EditionDelaiAutreDocumentFiscalView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"/>
	<tiles:put name="title">
		<c:set var="titleKey" value="title.enregistrement.demande.delai.docfisc"/>
		<fmt:message key="${titleKey}">
			<fmt:param>${ajouterView.perdiode}</fmt:param>
			<fmt:param><unireg:numCTB numero="${ajouterView.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<%--@elvariable id="ajouterView" type="ch.vd.unireg.documentfiscal.EditionDelaiAutreDocumentFiscalView"--%>
		<form:form method="post" name="theForm" id="formAddDelai" action="ajouter.do" modelAttribute="ajouterView">

			<form:errors cssClass="error"/>

			<form:hidden path="idDocumentFiscal"/>
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
						<td style="width: 25%;"><unireg:date date="${ajouterView.ancienDelaiAccorde}"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 25%;"></td>
						<td style="width: 25%;"></td>
						<td style="width: 25%;"><fmt:message key="label.date.delai.accorde"/>&nbsp;:</td>
						<td style="width: 25%;">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="delaiAccordeAu"/>
								<jsp:param name="id" value="delaiAccordeAu"/>
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>
				</table>

			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%">
						<input type="submit" id="ajouter" value="Ajouter">
					</td>
					<td width="25%">
						<unireg:buttonTo id="annuler" name="Annuler" action="/degrevement-exoneration/edit-demande-degrevement.do" method="get" params="{id:${ajouterView.idDocumentFiscal}}"/>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

		<script type="text/javascript">

			var DelaiPM = {

				verifierDelaiDI: function() {
					var dateDemande = '${ajouterView.dateDemande}';
					var delaiAccordeAu = $('#delaiAccordeAu').val();
					if (DateUtils.validate(delaiAccordeAu) && DateUtils.compare(DateUtils.addYear(dateDemande, 1, 'yyyy.MM.dd'), DateUtils.getDate(delaiAccordeAu, 'dd.MM.yyyy')) == -1) {
						return confirm("Ce délai est située plus d'un an dans le futur à compter de la date d'expédition de la DI. Voulez-vous le sauver ?");
					}
					return true;
				},

				ajouterDelai: function(button, type) {
					if (!this.verifierDelaiDI()) {
						return false;
					}

					$('#typeImpression').val(type);
					$('.error').hide();         // [SIFISC-18869] il faut enlever les éventuels messages d'erreur de l'affichage
					$(button).closest("form").submit();

					// On desactive les boutons
					$('#ajouter, #annuler, #envoi-auto, #envoi-manuel').hide();
					$('#retour').show();

					return true;
				}
			};

		</script>
	</tiles:put>
</tiles:insert>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="depuisTache" value="${param.depuisTache}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.di" /></tiles:put>
  	<tiles:put name="fichierAide"><a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-di.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a></tiles:put>
	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.tiersId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" />

		<form:form name="theForm" method="post" action="editer.do">

			<form:errors cssClass="error"/>

			<input type="hidden" name="id" value="${command.id}"/>
			<input type="hidden" name="tacheId" value="${command.tacheId}"/>

			<!-- Debut Declaration impot -->
			<fieldset class="information">
				<legend><span><fmt:message key="label.caracteristiques.di" /></span></legend>

				<table border="0">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
						<td width="25%">${command.periodeFiscale}</td>
						<td width="25%"><fmt:message key="label.code.controle"/>&nbsp;:</td>
						<td width="25%">${command.codeControle}</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.debut.periode.imposition" />&nbsp;:</td>
						<td width="25%"><unireg:regdate regdate="${command.dateDebutPeriodeImposition}"/></td>
						<td width="25%"><fmt:message key="label.date.fin.periode.imposition" />&nbsp;:</td>
						<td width="25%"><unireg:regdate regdate="${command.dateFinPeriodeImposition}"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.type.declaration" />&nbsp;:</td>
						<td width="25%">
							<c:if test="${!command.depuisTache && command.allowedQuittancement && command.typeDocument.ordinaire}">
								<%--@elvariable id="typesDeclarationImpotOrdinaire" type="java.util.Map<TypeDocument, String>"--%>
								<form:select path="typeDocument" items="${typesDeclarationImpotOrdinaire}" />
							</c:if>
							<c:if test="${command.depuisTache || !command.allowedQuittancement || !command.typeDocument.ordinaire}">
								<fmt:message key="option.type.document.${command.typeDocument}" />
							</c:if>
							<form:errors path="typeDocument" cssClass="error"/>
						</td>
						<td width="25%">&nbsp;</td>
						<td width="25%">&nbsp;</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.retour" />&nbsp;:</td>
						<td width="25%">
							<c:if test="${!command.depuisTache && command.allowedQuittancement}">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateRetour" />
									<jsp:param name="id" value="dateRetour" />
								</jsp:include>
							</c:if>
							<c:if test="${command.depuisTache || !command.allowedQuittancement}">
								<unireg:regdate regdate="${command.dateRetour}"/>
							</c:if>
							<form:errors path="dateRetour" cssClass="error"/>
						</td>
						<td width="25%"><fmt:message key="label.source"/>&nbsp;:</td>
						<td width="25%">
							<c:if test="${command.dateRetour != null}">
								<c:if test="${command.sourceQuittancement == null}">
									<fmt:message key="option.source.quittancement.UNKNOWN" />
								</c:if>
								<c:if test="${command.sourceQuittancement != null}">
									<fmt:message key="option.source.quittancement.${command.sourceQuittancement}" />
								</c:if>
							</c:if>
						</td>
					</tr>

				</table>

			</fieldset>
			<!-- Fin  Declaration impot -->

			<!-- Debut Delais -->
			<jsp:include page="../di/edit/delais.jsp"/>
			<!-- Fin Delais -->
		
			<!-- Debut Boutons -->
			<c:if test="${!command.depuisTache}">
				<input type="button" name="retourDI" value="<fmt:message key="label.bouton.retour" />" onclick="Page_RetourEdition(${command.tiersId});" />
				<c:if test="${command.allowedQuittancement}">
					<input type="submit" name="save" value="<fmt:message key="label.bouton.sauver" />" />
				</c:if>
				<c:if test="${command.allowedSommation}">
					<c:if test="${command.sommable}">
						<input type="button" name="sommer" value="<fmt:message key="label.bouton.sommer" />"  onclick="return Page_SommerDI(this);" />
					</c:if>
				</c:if>

				<!-- Duplicata DI -->

				<c:if test="${command.allowedDuplicata}">
					<input type="button" value="<fmt:message key="label.bouton.imprimer.duplicata" />" onclick="return open_imprime_di(${command.id});">
					<script type="text/javascript">
						function open_imprime_di(id) {
							var dialog = Dialog.create_dialog_div('imprime-di-dialog');

							// charge le contenu de la boîte de dialogue
							dialog.load(App.curl('/decl/duplicata.do') + '?id=' + id + '&' + new Date().getTime());

							dialog.dialog({
								title: "Impression d'un duplicata",
								height: 440,
								width: 500,
								modal: true,
								buttons: {
									"Imprimer": function() {
										// les boutons ne font pas partie de la boîte de dialogue (au niveau du DOM), on peut donc utiliser le sélecteur jQuery normal
										var buttons = $('.ui-button');
										buttons.each(function() {
											if ($(this).text() == 'Imprimer') {
												$(this).addClass('ui-state-disabled');
												$(this).attr('disabled', true);
											}
										});
										var form = dialog.find('#formImpression');
										form.attr('action', App.curl('/decl/duplicata.do'));
										form.submit();
									},
									"Fermer": function() {
										dialog.dialog("close");
									}
								}
							});
						}
					</script>

				</c:if>
				<!-- Impression de chemise de taxation d'office -->
				<c:if test="${command.allowedSommation && command.dernierEtat == 'ECHUE'}">
					<input type="submit" name="imprimerTO" value="<fmt:message key="label.bouton.imprimer.to" />" onclick="return Page_ImprimerTO(this);" />
				</c:if>
			</c:if>

			<c:if test="${command.depuisTache}">
				<unireg:buttonTo name="Retour" action="/tache/list.do" method="get" />
				<unireg:buttonTo name="Maintenir déclaration" action="/decl/maintenir.do" method="post" params="{tacheId:${command.tacheId}}"/>
			</c:if>

			<!-- Annulation DI -->
			<c:if test="${command.allowedSommation}">
				<unireg:buttonTo name="Annuler déclaration" confirm="Voulez-vous vraiment annuler cette déclaration d'impôt ?"
				                 action="/decl/annuler.do" method="post" params='{id:${command.id},depuisTache:${command.depuisTache}}'/>
			</c:if>

		<!-- Fin Boutons -->
		</form:form>

		<script type="text/javascript">
				/**
				 * Initialisation de l'observeur du flag 'modifier'
				 */
				Modifier.attachObserver( "theForm", false);
				Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver cette déclaration d\'impôt ?';
				Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver ?";

				function Page_RetourEdition(numero) {
					if (Modifier.isModified) {
						if (!confirm(Modifier.messageOverConfirmation)) {
							return false;
						}
					}
					document.location.href='<c:url value="/decl/list.do"/>?tiersId=' + numero ;
					return true;
				}

				function Page_SommerDI(button) {
					if(!confirm('Voulez-vous vraiment sommer cette déclaration d\'impôt ?')) {
						return false;
					}
					Form.dynamicSubmit('post', App.curl('/decl/sommer.do'), {id:${command.id}});
					button.disabled = true;
					return true;
			    }

			    function Page_ImprimerTO(button) {
					if(!confirm('Voulez-vous vraiment imprimer cette taxation d\'office ?'))
						return false;
				    Form.dynamicSubmit('post', App.curl('/decl/imprimerTO.do'), {id:${command.id}});
				    button.disabled = true;
				    return true;
			    }

		</script>
	</tiles:put>
</tiles:insert>

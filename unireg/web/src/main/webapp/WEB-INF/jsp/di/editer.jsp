<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="depuisTache" value="${param.depuisTache}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.di" /></tiles:put>
  	<tiles:put name="fichierAide"><li><a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-di.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>
	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.tiersId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" />

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
					<td width="25%"><fmt:message key="option.type.document.${command.typeDocument}"/></td>
					<td width="25%"><fmt:message key="label.etat.courant" />&nbsp;:</td>
					<td width="25%"><fmt:message key="option.etat.avancement.${command.dernierEtat}"/></td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin  Declaration impot -->

		<!-- Debut Delais -->
		<jsp:include page="delai/lister.jsp"/>
		<!-- Fin Delais -->

		<!-- Debut Etats -->
		<jsp:include page="etat/lister.jsp"/>
		<!-- Fin Etats -->

		<div style="margin-top:1em;">
			<!-- Debut Boutons -->
			<c:if test="${!command.depuisTache}">
				<unireg:buttonTo name="Retour" action="/di/list.do" method="get" params="{tiersId:${command.tiersId}}"/>
				<c:if test="${command.allowedSommation && command.sommable}">
					<input type="button" name="sommer" value="<fmt:message key="label.bouton.sommer" />"  onclick="return Page_SommerDI(this);" />
				</c:if>

				<!-- Duplicata DI -->

				<c:if test="${command.allowedDuplicata}">
					<input type="button" value="<fmt:message key="label.bouton.imprimer.duplicata" />" onclick="return open_imprime_di(${command.id});">
					<script type="text/javascript">
						function open_imprime_di(id) {
							var dialog = Dialog.create_dialog_div('imprime-di-dialog');

							// charge le contenu de la boîte de dialogue
							dialog.load(App.curl('/di/duplicata.do') + '?id=' + id + '&' + new Date().getTime());

							dialog.dialog({
								title: "Impression d'un duplicata",
								height: 350,
								width:  500,
								modal: true,
								buttons: {
									"Imprimer": function() {
										// les boutons ne font pas partie de la boîte de dialogue (au niveau du DOM), on peut donc utiliser le sélecteur jQuery normal

                                        var form = dialog.find('#formImpression');
                                        var radiosave = form.find('input[id=radio-save]:checked').val();
                                        var ischangetype = form.find('#changerType');
                                        //si aucun bouton radio sélèctionné avec changement de type , on lève un message d'erreur
                                        if (radiosave == null && ischangetype.attr("value") == 'true') {
                                            alert('Veuillez préciser votre choix concernant la sauvegarde de type de document');
                                        }
                                        else {

                                            var buttons = $('.ui-button');
                                            buttons.each(function () {
                                                if ($(this).text() == 'Imprimer') {
                                                    $(this).addClass('ui-state-disabled');
                                                    $(this).attr('disabled', true);
                                                }
                                            });


                                            form.attr('action', App.curl('/di/duplicata.do'));
                                            form.submit();
                                        }

									},
									"Fermer": function() {
										dialog.dialog("close");
									}
								}
							});
						}
					</script>

				</c:if>
			</c:if>

			<c:if test="${command.depuisTache}">
				<unireg:buttonTo name="Retour" action="/tache/list.do" method="get" />
			</c:if>

			<!-- Annulation DI -->
			<c:if test="${command.allowedSommation}">
				<c:if test="${command.tacheId != null}">
					<unireg:buttonTo name="Annuler déclaration" confirm="Voulez-vous vraiment annuler cette déclaration d'impôt ?"
					                 action="/di/annuler.do" method="post" params='{id:${command.id},tacheId:${command.tacheId}}'/>
				</c:if>
				<c:if test="${command.tacheId == null}">
					<unireg:buttonTo name="Annuler déclaration" confirm="Voulez-vous vraiment annuler cette déclaration d'impôt ?"
					                 action="/di/annuler.do" method="post" params='{id:${command.id}}'/>
				</c:if>
			</c:if>
		</div>

		<script type="text/javascript">
				function Page_SommerDI(button) {
					if(!confirm('Voulez-vous vraiment sommer cette déclaration d\'impôt ?')) {
						return false;
					}
					Form.dynamicSubmit('post', App.curl('/di/sommer.do'), {id:${command.id}});
					button.disabled = true;
					return true;
			    }
		</script>
	</tiles:put>
</tiles:insert>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="depuisTache" value="${param.depuisTache}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/di.js"/>"></script>
	</tiles:put>
	<tiles:put name="title"><fmt:message key="title.edition.di" /></tiles:put>
  	<tiles:put name="fichierAide"><li><a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-di.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>
	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<%--@elvariable id="command" type="ch.vd.unireg.di.view.EditerDeclarationImpotView"--%>
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
				<c:if test="${command.diPM}">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.debut.exercice.commercial" />&nbsp;:</td>
						<td width="25%"><unireg:regdate regdate="${command.dateDebutExerciceCommercial}"/></td>
						<td width="25%"><fmt:message key="label.date.fin.exercice.commercial" />&nbsp;:</td>
						<td width="25%"><unireg:regdate regdate="${command.dateFinExerciceCommercial}"/></td>
					</tr>
				</c:if>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.date.debut.periode.imposition" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${command.dateDebutPeriodeImposition}"/></td>
					<td width="25%"><fmt:message key="label.date.fin.periode.imposition" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${command.dateFinPeriodeImposition}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.type.declaration" />&nbsp;:</td>
					<td width="25%"><c:if test="${command.typeDocument != null}"><fmt:message key="option.type.document.${command.typeDocument}"/></c:if></td>
					<td width="25%"><fmt:message key="label.date.delai.imprimee" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${command.delaiRetourImprime}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.etat.courant" />&nbsp;:</td>
					<td width="25%"><fmt:message key="option.etat.avancement.f.${command.dernierEtat}"/></td>
					<td></td>
					<td></td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin  Declaration impot -->

		<!-- Debut Delais -->
		<c:choose>
			<c:when test="${command.diPP}">
				<jsp:include page="delai/lister-pp.jsp"/>
			</c:when>
			<c:when test="${command.diPM}">
				<jsp:include page="delai/lister-pm.jsp"/>
			</c:when>
		</c:choose>
		<!-- Fin Delais -->

		<!-- Debut Etats -->
		<jsp:include page="etat/lister.jsp"/>
		<!-- Fin Etats -->

		<!-- Debut Liberation -->
		<jsp:include page="liberation/lister-liberation.jsp">
			<jsp:param name="entite" value="LiberationDeclaration"/>
		</jsp:include>
		<!-- Fin Liberation -->

		<div style="margin-top:1em;">
			<!-- Debut Boutons -->
			<c:if test="${!command.depuisTache}">
				<unireg:buttonTo name="Retour" action="/di/list.do" id="boutonRetour" method="get" params="{tiersId:${command.tiersId}}"/>
				<c:if test="${command.allowedSommation && command.sommable}">
					<input type="button" name="sommer" value="<fmt:message key="label.bouton.sommer" />"  onclick="return sommerDI(${command.id});" />
					<script type="text/javascript">
						function sommerDI(id) {
							if(!confirm('Voulez-vous vraiment sommer cette déclaration d\'impôt ?')) {
								return false;
							}
							$(":button:not('#boutonRetour')").attr('disabled', true);
							Form.dynamicSubmit('post', App.curl('/di/sommer.do'), {id:id});
							return true;
						}
					</script>
				</c:if>

				<!-- Duplicata DI -->

				<c:if test="${command.allowedDuplicata}">
					<c:choose>

						<c:when test="${command.diPM}">
							<input type="button" id="bouton_duplicata_pm" value="<fmt:message key="label.bouton.imprimer.duplicata" />" onclick="return open_imprime_di_pm(${command.id});">
							<script type="text/javascript">
								function open_imprime_di_pm(id) {
									var dialog = Dialog.create_dialog_div('imprime-di-pm-dialog');

									// charge le contenu de la boîte de dialogue
									dialog.load(App.curl('/di/duplicata-pm.do') + '?id=' + id + '&' + new Date().getTime());

									dialog.dialog({
										              title: "Impression d'un duplicata",
										              height: 350,
										              width:  500,
										              modal: true,
										              buttons: {
											              "Imprimer": function() {
												              // les boutons ne font pas partie de la boîte de dialogue (au niveau du DOM), on peut donc utiliser le sélecteur jQuery normal

												              // correction des nombres de feuilles invalides
												              var form = dialog.find('#formImpression');
												              var invalidNumbers = form.find(':text').filter(function() {return !(/^[0-9]+/.test(this.value));});
												              invalidNumbers.val('0');

												              // il doit y avoir au moins une feuille de demandée
												              var nbtotal = 0;
												              form.find(":text").each(function() {nbtotal += Number($(this).val());});
												              if (nbtotal < 1) {
													              alert("Il faut sélectionner au moins une feuille à imprimer !");
													              return;
												              }

												              var buttons = $('.ui-button');
												              buttons.each(function () {
													              if ($(this).text() == 'Imprimer') {
														              $(this).addClass('ui-state-disabled');
														              $(this).attr('disabled', true);
													              }
												              });

												              form.attr('action', App.curl('/di/duplicata-pm.do'));
												              form.submit();
											              },
											              "Fermer": function() {
												              dialog.dialog("close");
											              }
										              }
									              });
								}
							</script>
						</c:when>

						<c:when test="${command.diPP}">
							<input type="button" value="<fmt:message key="label.bouton.imprimer.duplicata" />" onclick="return open_imprime_di_pp(${command.id});">
							<script type="text/javascript">
								function open_imprime_di_pp(id) {
									var dialog = Dialog.create_dialog_div('imprime-di-pp-dialog');

									// charge le contenu de la boîte de dialogue
									dialog.load(App.curl('/di/duplicata-pp.do') + '?id=' + id + '&' + new Date().getTime());

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
												if (radiosave == null && ischangetype.attr("value") === 'true') {
													alert('Veuillez préciser votre choix concernant la sauvegarde de type de document');
												}
												else {

													// correction des nombres de feuilles invalides
													var invalidNumbers = form.find(':text').filter(function() {return !(/^[0-9]+/.test(this.value));});
													invalidNumbers.val('0');

													// il doit y avoir au moins une feuille de demandée
													var nbtotal = 0;
													form.find(":text").each(function() {nbtotal += Number($(this).val());});
													if (nbtotal < 1) {
														alert("Il faut sélectionner au moins une feuille à imprimer !");
														return;
													}

													var buttons = $('.ui-button');
													buttons.each(function () {
														if ($(this).text() === 'Imprimer') {
															$(this).addClass('ui-state-disabled');
															$(this).attr('disabled', true);
														}
													});


													form.attr('action', App.curl('/di/duplicata-pp.do'));
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
						</c:when>
					</c:choose>
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

			<!-- Libération de la DI -->
			<c:if test="${command.allowedLiberation && !command.depuisTache}">
				<input type="button"  value="<fmt:message key="label.bouton.liberer.di" />" onclick="return di.creerModalLiberationDI(${command.id},'bouton_liberer_di','/di/liberer.do');">
			</c:if>
		</div>

	</tiles:put>
</tiles:insert>

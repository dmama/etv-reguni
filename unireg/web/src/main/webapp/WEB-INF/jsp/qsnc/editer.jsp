<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.questionnaire.snc" /></tiles:put>
	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${questionnaire.tiersId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" />

		<!-- Début questionnaire SNC -->
		<fieldset class="information">
			<legend><span><fmt:message key="label.caracteristiques.questionnaire.snc" /></span></legend>

			<table border="0">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
					<td width="25%">${questionnaire.periodeFiscale}</td>
					<td width="25%"><fmt:message key="label.etat.courant" />&nbsp;:</td>
					<td width="25%"><fmt:message key="option.etat.avancement.${questionnaire.etat}"/></td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin questionnaire SNC -->

		<!-- Debut délais -->
		<fieldset class="information">
			<legend><span><fmt:message key="label.delais"/></span></legend>
			<display:table 	name="questionnaire.delais" id="delai" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:column titleKey="label.date.demande" style="width: 30%;">
					<unireg:regdate regdate="${delai.dateDemande}" />
				</display:column>
				<display:column titleKey="label.date.delai.accorde" style="width: 30%;">
					<unireg:regdate regdate="${delai.delaiAccordeAu}" />
				</display:column>
				<display:column titleKey="label.date.traitement" style="width: 30%;">
					<unireg:regdate regdate="${delai.dateTraitement}" />
				</display:column>
				<display:column style="action">
					<unireg:consulterLog entityNature="DelaiDeclaration" entityId="${delai.id}"/>
				</display:column>

				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>
		</fieldset>
		<!-- Fin délais -->

		<!-- Debut états -->
		<fieldset>
			<legend><span><fmt:message key="label.etats"/></span></legend>

			<c:if test="${!depuisTache}">
				<authz:authorize ifAnyGranted="ROLE_QSNC_QUITTANCEMENT">
					<table id="quittancerBouton" border="0">
						<tr>
							<td>
								<unireg:linkTo name="Quittancer" title="Quittancer le questionnaire" action="/qsnc/ajouter-quittance.do" params="{id:${questionnaire.id}}" link_class="add margin_right_10"/>
							</td>
						</tr>
					</table>
				</authz:authorize>
			</c:if>

			<c:if test="${not empty questionnaire.etats}">
				<display:table name="questionnaire.etats" id="etat" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					<display:column titleKey="label.date.obtention" style="width: 30%;">
						<unireg:regdate regdate="${etat.dateObtention}"/>
						<c:if test="${!etat.annule && etat.etat == 'RAPPELEE'}">
							&nbsp;
							(<fmt:message key="label.date.envoi.courrier">
							<fmt:param><unireg:date date="${etat.dateEnvoiCourrier}"/></fmt:param>
						</fmt:message>)
						</c:if>
					</display:column>
					<display:column titleKey="label.etat" style="width: 30%;">
						<fmt:message key="option.etat.avancement.${etat.etat}"/>
					</display:column>
					<display:column titleKey="label.source" style="width: 30%;">
						<c:if test="${etat.etat == 'RETOURNEE'}">
							<c:if test="${etat.source == null}">
								<fmt:message key="option.source.quittancement.UNKNOWN"/>
							</c:if>
							<c:if test="${etat.source != null}">
								<fmt:message key="option.source.quittancement.${etat.source}"/>
							</c:if>
						</c:if>
					</display:column>
					<display:column style="action">
						<unireg:consulterLog entityNature="EtatDeclaration" entityId="${etat.id}"/>
						<authz:authorize ifAnyGranted="ROLE_QSNC_QUITTANCEMENT">
							<c:if test="${!etat.annule && etat.etat == 'RETOURNEE'}">
								<unireg:linkTo name="" title="Annuler le quittancement" confirm="Voulez-vous vraiment annuler ce quittancement ?"
								               action="/qsnc/annuler-quittance.do" method="post" params="{id:${etat.id}}" link_class="delete"/>
							</c:if>
						</authz:authorize>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</c:if>
		</fieldset>
		<!-- Fin états -->

		<div style="margin-top:1em;">
			<!-- Debut Boutons -->

			<!-- Bouton Retour -->
			<c:choose>
				<c:when test="${depuisTache}">
					<unireg:buttonTo name="Retour" action="/tache/list.do" id="boutonRetour" method="get"/>
				</c:when>
				<c:otherwise>
					<unireg:buttonTo name="Retour" action="/qsnc/list.do" id="boutonRetour" method="get" params="{tiersId:${questionnaire.tiersId}}"/>
				</c:otherwise>
			</c:choose>

			<!-- Bouton de rappel -->
			<authz:authorize ifAnyGranted="ROLE_QSNC_RAPPEL">
				<c:if test="${!depuisTache && !questionnaire.annule && questionnaire.rappelable}">
					<unireg:buttonTo name="Envoyer rappel" confirm="Voulez-vous réellement générer le courrier de rappel ?"
					                 action="/qsnc/rappel.do" method="post" params='{id:${questionnaire.id}}'/>
				</c:if>
			</authz:authorize>

			<!-- Bouton d'impression de duplicata -->
			<authz:authorize ifAnyGranted="ROLE_QSNC_DUPLICATA">
				<c:if test="${!depuisTache && !questionnaire.annule}">
					<unireg:buttonTo name="Duplicata" action="/qsnc/duplicata.do" method="post" params='{id:${questionnaire.id}}'/>
				</c:if>
			</authz:authorize>

			<!-- Bouton annulation de questionnaire -->
			<authz:authorize ifAnyGranted="ROLE_QSNC_EMISSION">
				<c:if test="${!questionnaire.annule}">
					<unireg:buttonTo name="Annuler questionnaire" confirm="Voulez-vous vraiment annuler ce questionnaire SNC ?"
					                 action="/qsnc/annuler.do" method="post" params='{id:${questionnaire.id},depuisTache:${depuisTache}}'/>
				</c:if>
			</authz:authorize>

		<%--<c:if test="${!command.depuisTache}">--%>
				<%--<unireg:buttonTo name="Retour" action="/qsnc/list.do" id="boutonRetour" method="get" params="{tiersId:${questionnaire.tiersId}}"/>--%>
				<%--<c:if test="${command.allowedSommation && command.sommable}">--%>
					<%--<input type="button" name="sommer" value="<fmt:message key="label.bouton.sommer" />"  onclick="return sommerDI(${command.id});" />--%>
					<%--<script type="text/javascript">--%>
						<%--function sommerDI(id) {--%>
							<%--if(!confirm('Voulez-vous vraiment sommer cette déclaration d\'impôt ?')) {--%>
								<%--return false;--%>
							<%--}--%>
							<%--$(":button:not('#boutonRetour')").attr('disabled', true);--%>
							<%--Form.dynamicSubmit('post', App.curl('/di/sommer.do'), {id:id});--%>
							<%--return true;--%>
						<%--}--%>
					<%--</script>--%>
				<%--</c:if>--%>

				<%--<!-- Duplicata DI -->--%>

				<%--<c:if test="${command.allowedDuplicata}">--%>
					<%--<c:choose>--%>

						<%--<c:when test="${command.diPM}">--%>
							<%--<input type="button" id="bouton_duplicata_pm" value="<fmt:message key="label.bouton.imprimer.duplicata" />" onclick="return open_imprime_di_pm(${command.id});">--%>
							<%--<script type="text/javascript">--%>
								<%--function open_imprime_di_pm(id) {--%>
									<%--var dialog = Dialog.create_dialog_div('imprime-di-pm-dialog');--%>

									<%--// charge le contenu de la boîte de dialogue--%>
									<%--dialog.load(App.curl('/di/duplicata-pm.do') + '?id=' + id + '&' + new Date().getTime());--%>

									<%--dialog.dialog({--%>
										              <%--title: "Impression d'un duplicata",--%>
										              <%--height: 350,--%>
										              <%--width:  500,--%>
										              <%--modal: true,--%>
										              <%--buttons: {--%>
											              <%--"Imprimer": function() {--%>
												              <%--// les boutons ne font pas partie de la boîte de dialogue (au niveau du DOM), on peut donc utiliser le sélecteur jQuery normal--%>

												              <%--// correction des nombres de feuilles invalides--%>
												              <%--var form = dialog.find('#formImpression');--%>
												              <%--var invalidNumbers = form.find(':text').filter(function() {return !(/^[0-9]+/.test(this.value));});--%>
												              <%--invalidNumbers.val('0');--%>

												              <%--// il doit y avoir au moins une feuille de demandée--%>
												              <%--var nbtotal = 0;--%>
												              <%--form.find(":text").each(function() {nbtotal += Number($(this).val());});--%>
												              <%--if (nbtotal < 1) {--%>
													              <%--alert("Il faut sélectionner au moins une feuille à imprimer !");--%>
													              <%--return;--%>
												              <%--}--%>

												              <%--var buttons = $('.ui-button');--%>
												              <%--buttons.each(function () {--%>
													              <%--if ($(this).text() == 'Imprimer') {--%>
														              <%--$(this).addClass('ui-state-disabled');--%>
														              <%--$(this).attr('disabled', true);--%>
													              <%--}--%>
												              <%--});--%>

												              <%--form.attr('action', App.curl('/di/duplicata-pm.do'));--%>
												              <%--form.submit();--%>
											              <%--},--%>
											              <%--"Fermer": function() {--%>
												              <%--dialog.dialog("close");--%>
											              <%--}--%>
										              <%--}--%>
									              <%--});--%>
								<%--}--%>
							<%--</script>--%>
						<%--</c:when>--%>

						<%--<c:when test="${command.diPP}">--%>
							<%--<input type="button" value="<fmt:message key="label.bouton.imprimer.duplicata" />" onclick="return open_imprime_di_pp(${command.id});">--%>
							<%--<script type="text/javascript">--%>
								<%--function open_imprime_di_pp(id) {--%>
									<%--var dialog = Dialog.create_dialog_div('imprime-di-pp-dialog');--%>

									<%--// charge le contenu de la boîte de dialogue--%>
									<%--dialog.load(App.curl('/di/duplicata-pp.do') + '?id=' + id + '&' + new Date().getTime());--%>

									<%--dialog.dialog({--%>
										<%--title: "Impression d'un duplicata",--%>
										<%--height: 350,--%>
										<%--width:  500,--%>
										<%--modal: true,--%>
										<%--buttons: {--%>
											<%--"Imprimer": function() {--%>
												<%--// les boutons ne font pas partie de la boîte de dialogue (au niveau du DOM), on peut donc utiliser le sélecteur jQuery normal--%>

												<%--var form = dialog.find('#formImpression');--%>
												<%--var radiosave = form.find('input[id=radio-save]:checked').val();--%>
												<%--var ischangetype = form.find('#changerType');--%>
												<%--//si aucun bouton radio sélèctionné avec changement de type , on lève un message d'erreur--%>
												<%--if (radiosave == null && ischangetype.attr("value") == 'true') {--%>
													<%--alert('Veuillez préciser votre choix concernant la sauvegarde de type de document');--%>
												<%--}--%>
												<%--else {--%>

													<%--// correction des nombres de feuilles invalides--%>
													<%--var invalidNumbers = form.find(':text').filter(function() {return !(/^[0-9]+/.test(this.value));});--%>
													<%--invalidNumbers.val('0');--%>

													<%--// il doit y avoir au moins une feuille de demandée--%>
													<%--var nbtotal = 0;--%>
													<%--form.find(":text").each(function() {nbtotal += Number($(this).val());});--%>
													<%--if (nbtotal < 1) {--%>
														<%--alert("Il faut sélectionner au moins une feuille à imprimer !");--%>
														<%--return;--%>
													<%--}--%>

													<%--var buttons = $('.ui-button');--%>
													<%--buttons.each(function () {--%>
														<%--if ($(this).text() == 'Imprimer') {--%>
															<%--$(this).addClass('ui-state-disabled');--%>
															<%--$(this).attr('disabled', true);--%>
														<%--}--%>
													<%--});--%>


													<%--form.attr('action', App.curl('/di/duplicata-pp.do'));--%>
													<%--form.submit();--%>
												<%--}--%>

											<%--},--%>
											<%--"Fermer": function() {--%>
												<%--dialog.dialog("close");--%>
											<%--}--%>
										<%--}--%>
									<%--});--%>
								<%--}--%>
							<%--</script>--%>
						<%--</c:when>--%>
					<%--</c:choose>--%>
				<%--</c:if>--%>
			<%--</c:if>--%>

			<%--<c:if test="${command.depuisTache}">--%>
				<%--<unireg:buttonTo name="Retour" action="/tache/list.do" method="get" />--%>
			<%--</c:if>--%>

			<%--<!-- Annulation DI -->--%>
			<%--<c:if test="${command.allowedSommation}">--%>
				<%--<c:if test="${command.tacheId != null}">--%>
					<%--<unireg:buttonTo name="Annuler déclaration" confirm="Voulez-vous vraiment annuler cette déclaration d'impôt ?"--%>
					                 <%--action="/di/annuler.do" method="post" params='{id:${command.id},tacheId:${command.tacheId}}'/>--%>
				<%--</c:if>--%>
				<%--<c:if test="${command.tacheId == null}">--%>
					<%--<unireg:buttonTo name="Annuler déclaration" confirm="Voulez-vous vraiment annuler cette déclaration d'impôt ?"--%>
					                 <%--action="/di/annuler.do" method="post" params='{id:${command.id}}'/>--%>
				<%--</c:if>--%>
			<%--</c:if>--%>
		</div>

	</tiles:put>
</tiles:insert>

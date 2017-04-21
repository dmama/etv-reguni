<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>
<c:set var="printparam" value='<%= request.getParameter("printview") %>' />
<c:set var="printview" value="${(empty printparam)? false :printparam }" />

<c:set var="layout" value="${param.layout}" />
<c:if test="${layout != null && layout == 'dialog'}">
	<c:set var="templatePath" value="/WEB-INF/jsp/templates/templateDialog.jsp" /> <%-- [SIFISC-6223] --%>
</c:if>
<c:if test="${layout == null || layout != 'dialog'}">
	<c:set var="templatePath" value="/WEB-INF/jsp/templates/template.jsp" />
</c:if>

<tiles:insert template="${templatePath}">
	<tiles:put name="title"><fmt:message key="title.visualisation.tiers" /></tiles:put>
	<tiles:put name="displayedTitleSuffix">
		<span style="display:none;" id="print-button">
			<unireg:raccourciImprimer onClick="window.print();" tooltip="Imprimer la page"/>
		</span>
	</tiles:put>
	
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/visualisation.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	
	<tiles:put name="vue">
		<li>
			<a href="#" onclick="modeImpression(false)">
				<span class="form-friendly" style="display: none;" id="tabnav-enable"><fmt:message key="label.vue.ecran" /></span>
			</a>
			<a href="#" onclick="modeImpression(true)">
				<span class="printer-friendly" style="display: block;" id="tabnav-disable"><fmt:message key="label.vue.imprimable" /></span>
			</a>
		</li>
	</tiles:put>

	<tiles:put name="body">
	
	<%--@elvariable id="warnings" type="java.util.List<String>"--%>
	<c:if test="${not empty warnings}">
		<table class="warnings iepngfix" cellspacing="0" cellpadding="0" border="0">
			<tr><td class="heading"><fmt:message key="label.action.avertissements"/></td></tr>
			<tr><td class="details"><ul>
			<c:forEach var="warn" items="${warnings}">
				<li class="warn"><c:out value="${warn}"/></li>
			</c:forEach>
			</ul></td></tr>
		</table>
	</c:if>

	<c:if test="${command.tiers != null}">

		<unireg:nextRowClass reset="1"/>

		<c:if test="${layout == null || layout != 'dialog'}"> <%-- [SIFISC-6223] --%>
			<authz:authorize ifAnyGranted="ROLE_SUPERGRA">
				<div style="position:relative;">
					<div style="position:absolute; top:-1.5em; right:0;" class="noprint">
						<a href="<c:url value="/supergra/entity/show.do?id=${command.tiersGeneral.numero}&class=Tiers"/>">Edition de ce tiers en mode SuperGra</a>
					</div>
				</div>
			</authz:authorize>

			<!-- Debut Caracteristiques generales -->
			<jsp:include page="../../general/tiers.jsp">
				<jsp:param name="page" value="visu" />
				<jsp:param name="path" value="tiersGeneral" />
				<jsp:param name="idBandeau" value="bandeauTiers" />
			</jsp:include>
			<!-- Fin Caracteristiques generales -->
		</c:if>

		<!--onglets-->
		<div id="tiersTabs">
			<ul id="menuTiersTabs">
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_VISU_FORS">
					<c:if test="${command.natureTiers != 'Etablissement'}">
						<li id="fiscalTab">
							<a href="#tabContent_fiscalTab"><fmt:message key="label.fiscal" /></a>
						</li>
					</c:if>
				</authz:authorize>
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
						<li id="civilTab">
							<a href="#tabContent_civilTab">
								<fmt:message key="label.civil"/>
								<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD">
									<c:if test="${command.withCanceledIndividu}">
										<span class="ongletCivilAvecIndividuAnnule" title="<fmt:message key="label.individu.annule.tooltip"/>">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
									</c:if>
								</authz:authorize>
							</a>
						</li>
					</c:if>
				</authz:authorize>
				<li id="adressesTab">
					<a href="#tabContent_adressesTab"><fmt:message key="label.adresse" /></a>
				</li>
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<li id="complementsTab">
						<a href="#tabContent_complementsTab"><fmt:message key="label.complements" /></a>
					</li>
					<c:if test="${command.natureTiers == 'DebiteurPrestationImposable'}">
						<li id="rapportsPrestationTab">
							<a href="#tabContent_rapportsPrestationTab"><fmt:message key="label.dossiers.apparentes" /></a>
						</li>
						<li id="lrTab">
							<a href="#tabContent_lrTab"><fmt:message key="label.lr" /></a>
						</li>
					</c:if>
				</authz:authorize>
				<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
					<li id="dossiersApparentesTab">
						<a href="#tabContent_dossiersApparentesTab"><fmt:message key="label.dossiers.apparentes" /></a>
					</li>
					<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
						<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun' || command.natureTiers == 'Entreprise'}">
							<li id="diTab">
								<a href="#tabContent_diTab"><fmt:message key="label.di" /></a>
							</li>
						</c:if>
						<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun'}">
							<li id="mouvementTab">
								<a href="#tabContent_mouvementTab"><fmt:message key="label.mouvement" /></a>
							</li>
						</c:if>
						<c:if test="${command.natureTiers == 'Entreprise'}">
							<c:if test="${command.entreprise.isOrWasSocieteDePersonnes || not empty command.questionnairesSNC}">
								<li id="qsncTab">
									<a href="#tabContent_qsncTab"><fmt:message key="label.questionnaires.snc"/></a>
								</li>
							</c:if>
							<li id="autresDocumentsFiscauxTab">
								<a href="#tabContent_autresDocumentsTab"><fmt:message key="label.autres.documents.fiscaux"/></a>
							</li>
							<li id="mandatairesTab">
								<a href="#tabContent_mandatairesTab"><fmt:message key="label.mandataires"/></a>
							</li>
							<li id="specificitesFiscalesTab">
								<a href="#tabContent_specificitesFiscalesTab"><fmt:message key="label.specificites.fiscales"/></a>
							</li>
							<li id="etatsPMTab">
								<a href="#tabContent_etatsPMTab"><fmt:message key="label.etats.pm" /></a>
							</li>
						</c:if>
					</authz:authorize>
				</c:if>

				<unireg:ifEfacture>
					<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun'}">
						<authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_GEST_EFACTURE">
							<li id="efactureTab">
								<a href="#tabContent_efactureTab"><span><fmt:message key="label.efacture"/></span></a>
							</li>
						</authz:authorize>
					</c:if>
				</unireg:ifEfacture>

				<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun'}">
					<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
						<li id="etiquetteTab">
							<a href="#tabContent_etiquetteTab"><span><fmt:message key="label.etiquettes"/></span></a>
						</li>
					</authz:authorize>
				</c:if>

				<c:if test="${command.natureTiers == 'Entreprise'}">
					<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
						<li id="degrevementExoTab">
							<a href="#tabContent_degrevementExoTab"><span><fmt:message key="label.degrevements.exonerations"/></span></a>
						</li>
					</authz:authorize>
				</c:if>

				<authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_REMARQUE_TIERS">
					<li id="remarqueTab">
						<a id="remarqueTabAnchor" href="#tabContent_remarqueTab"><fmt:message key="label.remarques" /></a>
					</li>
				</authz:authorize>
			</ul>

			<authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_VISU_FORS">
				<c:if test="${command.natureTiers != 'Etablissement'}">
					<div id="tabContent_fiscalTab" class="situation_fiscale">
						<jsp:include page="fiscal/fiscal.jsp"/>
					</div>
				</c:if>
			</authz:authorize>
			<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
				<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
					<div id="tabContent_civilTab" class="editTiers">
						<jsp:include page="civil/civil.jsp"/>
					</div>
				</c:if>
			</authz:authorize>
			<div id="tabContent_adressesTab" class="adresses">
				<jsp:include page="adresse/adresse.jsp"/>
			</div>
			<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
				<div id="tabContent_complementsTab" class="editTiers">
					<jsp:include page="complement.jsp"/>
				</div>
				<c:if test="${command.natureTiers == 'DebiteurPrestationImposable'}">
					<div id="tabContent_rapportsPrestationTab" class="visuTiers">
						<jsp:include page="rapports-prestation.jsp"/>
					</div>
					<div id="tabContent_lrTab" class="visuTiers">
						<jsp:include page="lr/lrs.jsp"/>
					</div>
				</c:if>
			</authz:authorize>

			<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">

				<div id="tabContent_dossiersApparentesTab" class="visuTiers">
					<jsp:include page="dossiers-apparentes.jsp"/>
				</div>
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun' || command.natureTiers == 'Entreprise'}">
						<div id="tabContent_diTab" class="visuTiers">
							<c:choose>
								<c:when test="${command.natureTiers == 'Entreprise'}">
									<jsp:include page="di/dis-pm.jsp"/>
								</c:when>
								<c:otherwise>
									<jsp:include page="di/dis.jsp"/>
								</c:otherwise>
							</c:choose>
						</div>
					</c:if>
					<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun'}">
						<div id="tabContent_mouvementTab" class="visuTiers">
							<jsp:include page="mouvement/mouvements.jsp"/>
						</div>
					</c:if>
					<c:if test="${command.natureTiers == 'Entreprise'}">
						<c:if test="${command.entreprise.isOrWasSocieteDePersonnes || not empty command.questionnairesSNC}">
							<div id="tabContent_qsncTab" class="visuTiers">
								<jsp:include page="pm/qsnc.jsp"/>
							</div>
						</c:if>
						<div id="tabContent_autresDocumentsTab" class="visuTiers">
							<jsp:include page="pm/autresdocs.jsp"/>
						</div>
						<div id="tabContent_mandatairesTab" class="visuTiers">
							<jsp:include page="pm/mandataires.jsp"/>
						</div>
						<div id="tabContent_specificitesFiscalesTab" class="visuTiers">
							<jsp:include page="pm/specificites.jsp"/>
						</div>
						<div id="tabContent_etatsPMTab" class="visuTiers">
							<jsp:include page="pm/etats.jsp"/>
						</div>
					</c:if>
				</authz:authorize>
			</c:if>

			<unireg:ifEfacture>
	            <c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun'}">
	                <authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_GEST_EFACTURE">
	                    <div id="tabContent_efactureTab" class="visuTiers">
	                        <jsp:include page="efacture.jsp"/>
	                    </div>
	                </authz:authorize>
	            </c:if>
			</unireg:ifEfacture>

			<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun'}">
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<div id="tabContent_etiquetteTab" class="visuTiers">
						<jsp:include page="etiquette.jsp"/>
					</div>
				</authz:authorize>
			</c:if>

			<c:if test="${command.natureTiers == 'Entreprise'}">
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<div id="tabContent_degrevementExoTab" class="visuTiers">
						<jsp:include page="pm/degrevement-exoneration/degrevement-exoneration.jsp"/>
					</div>
				</authz:authorize>
			</c:if>

            <authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_REMARQUE_TIERS">
				<div id="tabContent_remarqueTab" class="visuTiers">
						<jsp:include page="../common/remarque/remarques.jsp">
							<jsp:param name="tiersId" value="${command.tiersGeneral.numero}" />
						</jsp:include>
				</div>
			</authz:authorize>
		</div>

		<script type="text/javascript">
			$(function() {
				$("#tiersTabs").tabs({cookie:{}, cache:true, spinner:"<em>Chargement&#8230;</em>"});

				// [SIFISC-2587] sélection automatique d'un onglet (cas général)
				var params = App.get_url_params();
				if (params && params.selectTab) {
					try {
						var index = $('#' + params.selectTab).index();
						$('#tiersTabs').tabs('select', index);
					}
					catch (err) {
						// tant pis
					}
				}
			});
		</script>

		<!-- Fin onglets -->

		<!-- Debut Boutons -->
		<form:form>
		<c:set var="onClickBoutonRetour" value="document.location='list.do';"/>
		<c:if test="${not empty param['urlRetour']}">
			<c:set var="onClickBoutonRetour" value="document.location='${param['urlRetour']}&onglet=rapportsPrestationTab';"/>
		</c:if>
		<c:if test="${not empty param['message']}">
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location='../identification/gestion-messages/edit.do?source=${param['source']}&keepCriteria=true&id=${param['message']}'" />
		</c:if>
		<c:if test="${not empty param['retour']}">
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location='../identification/gestion-messages/listTraite.do?keepCriteria=true'" />
		</c:if>
		</form:form>
		<!-- Fin Boutons -->
		&nbsp;
	</c:if>
	<c:if test="${command.tiers == null}">
		<c:if test="${command.allowed}">
			<span class="error"><fmt:message key="error.tiers.inexistant" /></span>
		</c:if>
		<c:if test="${!command.allowed}">
			<span class="error"><fmt:message key="error.tiers.interdit" /></span>
		</c:if>
	</c:if>

	<script type="text/javascript" language="Javascript1.3">
			function Page_AnnulerTiers(ev) {
				if(!confirm('Voulez-vous vraiment annuler ce tiers ?'))
					return Event.stop(ev);
				return true;
		 	}

		 	/**
		 	 * Vue imprimable : affichage les contenus des tabs l'un dessus l'autre
		 	 */
		 	function showPrintView() {
		 		$('#menuTiersTabs').css('display', 'none');
		 		$('.ui-tabs-hide').addClass('tabs_previously_hidden');
		 		$('.ui-tabs-hide').removeClass('ui-tabs-hide');
		 		$("#tabnav-disable").hide();
		 		$("#tabnav-enable").show();
			    $('#print-button').show();

		 	}

			function modeImpression(actif){
				var url = updateQueryStringParameter(window.location.href,'printview',actif);
				window.location.replace(url);
			}

		 	/**
		 	 * Vue normale : affichage les tabs normalement.
		 	 */
		 	function showScreenView() {
		 		$('#menuTiersTabs').css('display', ''); // [SIFISC-93] on n'utilise pas show() qui met le display à 'inline-block' et du coup on voit les éléments lors de l'impression
		 		$('.tabs_previously_hidden').addClass('ui-tabs-hide');
		 		$('.tabs_previously_hidden').removeClass('tabs_previously_hidden');
		 		$("#tabnav-disable").show();
		 		$("#tabnav-enable").hide();
			    $('#print-button').hide();
			}

			function updateQueryStringParameter(uri, key, value) {

				// remove the hash part before operating on the uri
				var i = uri.indexOf('#');
				var hash = i === -1 ? '' : uri.substr(i);
				uri = i === -1 ? uri : uri.substr(0, i);

				var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
				var separator = uri.indexOf('?') !== -1 ? "&" : "?";
				if (uri.match(re)) {
					uri = uri.replace(re, '$1' + key + "=" + value + '$2');
				} else {
					uri = uri + separator + key + "=" + value;
				}
				return uri + hash;
			}

		 	// [UNIREG-3290] Sélection de la vue imprimable par l'url
			$(function() {
				var url = document.location.toString();
				//par défaut
				showScreenView();
				if (url.indexOf("printview=true") != -1) {
					showPrintView();
				}
			});
	</script>

	<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
		<script type="text/javascript" language="Javascript1.3">
				Histo.toggleRowsIsHisto('situationFamille','isSFHisto', 5);
				Histo.toggleRowsIsHisto('dossierApparente','isRapportHisto', 2);
				Histo.toggleRowsIsHisto('adresse','isAdrHisto',2);
				Histo.toggleRowsIsHisto('adresseCivile','isAdrHistoCiviles',3);
				Histo.toggleRowsIsHisto('adresseCivileConjoint','isAdrHistoCivilesConjoint',3);
		</script>
	</c:if>
	<c:if test="${command.natureTiers == 'DebiteurPrestationImposable'}">
		<script type="text/javascript" language="Javascript1.3">
			Histo.toggleRowsIsHisto('adresse','isAdrHisto',2);
			Histo.toggleAffichageRows('forFiscal',${printview}, 2);
			Histo.toggleRowsIsHistoFromClass('periodicite', 'isPeriodiciteHisto', 'histo-only');
		</script>
	</c:if>

	</tiles:put>
	
</tiles:insert>

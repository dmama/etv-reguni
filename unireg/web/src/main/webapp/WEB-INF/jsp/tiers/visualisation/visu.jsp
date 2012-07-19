<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.visualisation.tiers" /></tiles:put>
	
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/visualisation.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	
	<tiles:put name="vue">
		<li>
			<a href="#" onclick="showScreenView()">
				<span class="form-friendly" style="display: none;" id="tabnav-enable"><fmt:message key="label.vue.ecran" /></span>
			</a>
			<a href="#" onclick="showPrintView()">
				<span class="printer-friendly" style="display: block;" id="tabnav-disable"><fmt:message key="label.vue.imprimable" /></span>
			</a>
		</li>
	</tiles:put>

	<tiles:put name="body">
	
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

		<authz:authorize ifAnyGranted="ROLE_SUPERGRA">
			<div style="position:relative;">
				<div style="position:absolute; top:-1.5em; right:0px;" class="noprint">
					<a href="<c:url value="/supergra/entity.do?id=${command.tiersGeneral.numero}&class=Tiers"/>">Edition de ce tiers en mode SuperGra</a>
				</div>
			</div>
		</authz:authorize>

		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/tiers.jsp">
			<jsp:param name="page" value="visu" />
			<jsp:param name="path" value="tiersGeneral" />		
		</jsp:include>
		<!-- Fin Caracteristiques generales -->

		<!--onglets-->
		<div id="tiersTabs">
			<ul id="menuTiersTabs">
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_VISU_FORS">
					<li id="fiscalTab">
						<a href="#tabContent_fiscalTab"><fmt:message key="label.fiscal" /></a>
					</li>
				</authz:authorize>
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
						<li id="civilTab">
							<a href="#tabContent_civilTab"><fmt:message key="label.civil" /></a>
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
						<c:if test="${command.natureTiers != 'Entreprise'}">
							<li id="diTab">
								<a href="#tabContent_diTab"><fmt:message key="label.di" /></a>
							</li>
							<li id="mouvementTab">
								<a href="#tabContent_mouvementTab"><fmt:message key="label.mouvement" /></a>
							</li>
						</c:if>
						<c:if test="${command.natureTiers == 'Entreprise'}">
							<li id="regimesFiscauxTab">
								<a href="#tabContent_regimesFiscauxTab"><fmt:message key="label.regimes.fiscaux" /></a>
							</li>
							<li id="etatsPMTab">
								<a href="#tabContent_etatsPMTab"><fmt:message key="label.etats.pm" /></a>
							</li>
						</c:if>
					</authz:authorize>
				</c:if>

				<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant'}">
					<authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_VISU_IMMEUBLES">
						<li id="immeublesTab">
							<a href="#tabContent_immeublesTab"><span><fmt:message key="label.immeubles" /></span></a>
						</li>
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

				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<li id="remarqueTab">
						<a id="remarqueTabAnchor" href="#tabContent_remarqueTab"><fmt:message key="label.remarques" /></a>
					</li>
				</authz:authorize>
			</ul>

			<authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_VISU_FORS">
				<div id="tabContent_fiscalTab" class="situation_fiscale">
					<jsp:include page="fiscal/fiscal.jsp"/>
				</div>
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
					<c:if test="${command.natureTiers != 'Entreprise'}">
						<div id="tabContent_diTab" class="visuTiers">
							<jsp:include page="di/dis.jsp"/>
						</div>
						<div id="tabContent_mouvementTab" class="visuTiers">
							<jsp:include page="mouvement/mouvements.jsp"/>
						</div>
					</c:if>
					<c:if test="${command.natureTiers == 'Entreprise'}">
						<div id="tabContent_regimesFiscauxTab" class="visuTiers">
							<jsp:include page="pm/regimes-fiscaux.jsp"/>
						</div>
						<div id="tabContent_etatsPMTab" class="visuTiers">
							<jsp:include page="pm/etats.jsp"/>
						</div>
					</c:if>
				</authz:authorize>
			</c:if>

			<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant'}">
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL, ROLE_VISU_IMMEUBLES">
					<div id="tabContent_immeublesTab" class="visuTiers">
						<jsp:include page="immeubles.jsp"/>
					</div>
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

            <authz:authorize ifAnyGranted="ROLE_VISU_ALL">
				<div id="tabContent_remarqueTab" class="visuTiers">
						<jsp:include page="../common/remarque/remarques.jsp">
							<jsp:param name="tiersId" value="${command.tiersGeneral.numero}" />
						</jsp:include>
				</div>
			</authz:authorize>
		</div>
		<script>
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
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location='../identification/gestion-messages/edit.do?id=${param['message']}'" />
		</c:if>
		<c:if test="${not empty param['retour']}">
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location='../identification/gestion-messages/listTraite.do'" />
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
		 	}

		 	// [UNIREG-3290] Sélection de la vue imprimable par l'url
			$(function() {
				var url = document.location.toString();
				if (url.indexOf("printview=true") != -1) {
					showPrintView();
				}
			});
	</script>

	<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
		<script type="text/javascript" language="Javascript1.3">
				Histo.toggleAffichageRows('forFiscal',false, 6);
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
			Histo.toggleAffichageRows('forFiscal',false, 2);
			Histo.toggleRowsIsHistoPeriodicite('periodicite','isPeriodiciteHisto', 2,3);
			Histo.toggleRowsIsActif('contribuableAssocie','isCtbAssoHisto', 0);
		</script>
	</c:if>

	</tiles:put>
	
</tiles:insert>

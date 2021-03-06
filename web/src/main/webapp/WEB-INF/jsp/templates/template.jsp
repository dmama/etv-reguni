<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<html>
	<head>
		<!-- demandons à IE d'utiliser le dernier moteur -->
		<meta http-equiv="X-UA-Compatible" content="IE=edge" />

		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<tiles:getAsString name='refresh' ignore='true'/>
		<link rel="SHORTCUT ICON" href="<c:url value="/images/favicon.ico"/>">

		<%@ include file="cssInclude.jsp" %>
		<%@ include file="/WEB-INF/jsp/include/fieldsets-workaround.jsp" %>

		<script type="text/javascript" language="javascript">
            function ouvrirAide(url) {
                window.open(url, "aide", "top=100, left=750, screenY=50, screenX=100, width=500, height=800, directories=no, location=no, menubar=no, status=no, toolbar=no, resizable=yes");
            }
        </script>

		<%@ include file="jsInclude.jsp" %>

		<title><tiles:getAsString name='title' ignore='false'/></title>
		<tiles:getAsString name='head' ignore='true'/>
	</head>
	<body>
			<fmt:setLocale value="fr_CH" scope="session"/>
			<div id="sommaire">
				<div style="position:absolute; top:5px; left:5px;">
					<div id="loadingImage" style="display:none;"><img src="<c:url value="/images/loading.gif"/>"  alt="loading..."/></div>
				</div>
				<script type="text/javascript">
					App.init('<c:url value="/"/>');
					Navigation.init('<c:url value="/"/>');
					Navigation.onShow(window.location.href);
				</script>
				<div class="canton">
					<a href="http://www.vd.ch" target="_blank">
						<span class="label"><fmt:message key="label.canton.vaud" /></span>
					</a>
				</div>
				<h3>Sommaire&nbsp;<a href="#" onClick="ouvrirAide('<c:url value="/docs/sommaire-glossaire.pdf"/>');" title="AccessKey: a" accesskey="e">?</a>
				</h3>
				<tiles:getAsString name='links' ignore='true'/>
				<ul>
					<li><a href="<c:url value='/tiers/list.do'/>"><fmt:message key="title.rechercher" /></a></li>

					<authz:authorize access="hasAnyRole('VISU_ALL')">
						<li>
							<a href="<c:url value='/admin/inbox/show.do'/>">
								<span id="inboxSize"><fmt:message key="title.inbox"/></span>
							</a>
						</li>

						<script>
							function refreshInboxSize() { // attention, cette méthode est appelée depuis la page inbox.jsp !
								Inbox.refreshSize($('#inboxSize'), '<fmt:message key="title.inbox"/>');
							}
							// dès l'affichage de la page, on rafraîchit le nombre d'éléments non lus de l'inbox
							$(refreshInboxSize);
						</script>

					</authz:authorize>

					<authz:authorize var="creationpp" access="hasAnyRole('CREATE_NONHAB', 'CREATE_AC')"/>
					<authz:authorize var="modifpp" access="hasAnyRole('MODIF_VD_ORD', 'MODIF_VD_SOURC', 'MODIF_HC_HS', 'MODIF_HAB_DEBPUR', 'MODIF_NONHAB_DEBPUR')"/>
					<authz:authorize var="annulpp" access="hasRole('ANNUL_TIERS') and ${modifpp}"/>

					<c:if test="${creationpp || modifpp || annulpp}">
						<li><fmt:message key="title.personnes.physiques"/>
							<ul>
								<authz:authorize access="hasAnyRole('CREATE_NONHAB')">
									<li><a href="<c:url value='/tiers/nonhabitant/create.do'/>"><fmt:message key="title.creation.inconnu.controle.habitants" /></a></li>
								</authz:authorize>
								<authz:authorize access="hasAnyRole('CREATE_AC')">
									<li><a href="<c:url value='/tiers/autrecommunaute/create.do'/>"><fmt:message key="title.creation.autre.communaute" /></a></li>
								</authz:authorize>
								<c:if test="${annulpp}">
									<li><a href="<c:url value='/activation/list.do?mode=DESACTIVATION&population=PP'/>"><fmt:message key="label.action.annulation" /></a></li>
									<li><a href="<c:url value='/activation/list.do?mode=REACTIVATION&population=PP'/>"><fmt:message key="title.reactivation.tiers" /></a></li>
								</c:if>
								<c:if test="${modifpp}">
									<li><fmt:message key="label.action.processus.complexes" />
										<ul>
											<li><a href="<c:url value='/couple/create.do'/>"><fmt:message key="title.couple" /></a></li>
											<li><a href="<c:url value='/separation/list.do'/>"><fmt:message key="title.separation" /></a></li>
											<li><a href="<c:url value='/deces/list.do'/>"><fmt:message key="title.deces" /></a></li>
										</ul>
									</li>
									<li><fmt:message key="label.action.annulation.processus.complexes" />
										<ul>
											<c:if test="${modifpp}">
												<li><a href="<c:url value='/annulation/couple/list.do'/>"><fmt:message key="title.couple" /></a></li>
												<li><a href="<c:url value='/annulation/separation/list.do'/>"><fmt:message key="title.separation" /></a></li>
												<li><a href="<c:url value='/annulation/deces/list.do'/>"><fmt:message key="title.deces" /></a></li>
											</c:if>
										</ul>
									</li>
								</c:if>
							</ul>
						</li>
					</c:if>

					<authz:authorize access="hasAnyRole('LR')">
						<li><a href="<c:url value='/lr/list.do'/>"><fmt:message key="title.lr" /></a></li>
					</authz:authorize>

					<authz:authorize var="creationent" access="hasRole('CREATE_ENTREPRISE')"/>
					<authz:authorize var="proccomplexeent" access="hasAnyRole('FAILLITE_ENTREPRISE, DEMENAGEMENT_SIEGE_ENTREPRISE, FIN_ACTIVITE_ENTREPRISE, FUSION_ENTREPRISES, SCISSION_ENTREPRISE, TRANSFERT_PATRIMOINE_ENTREPRISE, REINSCRIPTION_RC_ENTREPRISE, REQUISITION_RADIATION_RC')"/>
					<authz:authorize var="annulproccomplexeent" access="hasAnyRole('FAILLITE_ENTREPRISE, DEMENAGEMENT_SIEGE_ENTREPRISE, FIN_ACTIVITE_ENTREPRISE, FUSION_ENTREPRISES, SCISSION_ENTREPRISE, TRANSFERT_PATRIMOINE_ENTREPRISE')"/>
					<authz:authorize var="annulent" access="hasRole('ANNUL_TIERS') and hasRole('MODIF_PM')"/>

					<c:if test="${creationent || proccomplexeent || annulproccomplexeent || annulent}">
						<li><fmt:message key="title.entreprises"/>
							<ul>
								<c:if test="${creationent}">
									<li><a href="<c:url value='/tiers/entreprise/create.do'/>"><fmt:message key="label.action.creation" /></a></li>
								</c:if>
								<c:if test="${annulent}">
									<li><a href="<c:url value='/activation/list.do?mode=DESACTIVATION&population=PM'/>"><fmt:message key="label.action.annulation" /></a></li>
									<li><a href="<c:url value='/activation/list.do?mode=REACTIVATION&population=PM'/>"><fmt:message key="title.reactivation.tiers" /></a></li>
								</c:if>
								<c:if test="${proccomplexeent}">
									<li><fmt:message key="label.action.processus.complexes"/>
										<ul>
											<authz:authorize access="hasAnyRole('FAILLITE_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/faillite/list.do"/>"><fmt:message key="title.faillite"/></a></li>
												<li><a href="<c:url value="/processuscomplexe/revocation/faillite/list.do"/>"><fmt:message key="title.revocation.faillite"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('DEMENAGEMENT_SIEGE_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/demenagement/list.do"/>"><fmt:message key="title.demenagement.siege"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('FIN_ACTIVITE_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/finactivite/list.do"/>"><fmt:message key="title.fin.activite"/></a></li>
												<li><a href="<c:url value="/processuscomplexe/repriseactivite/list.do"/>"><fmt:message key="title.reprise.partielle.activite"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('FUSION_ENTREPRISES')">
												<li><a href="<c:url value="/processuscomplexe/fusion/absorbante/list.do"/>"><fmt:message key="title.fusion.entreprises"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('SCISSION_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/scission/scindee/list.do"/>"><fmt:message key="title.scission.entreprise"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('TRANSFERT_PATRIMOINE_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/transfertpatrimoine/emettrice/list.do"/>"><fmt:message key="title.transfert.patrimoine"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('REQUISITION_RADIATION_RC')">
												<li><a href="<c:url value="/processuscomplexe/requisitionradiationrc/list.do"/>"><fmt:message key="title.requisition.radiation.rc"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('REINSCRIPTION_RC_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/reinscriptionrc/list.do"/>"><fmt:message key="title.reinscription.rc"/></a></li>
											</authz:authorize>
										</ul>
									</li>
								</c:if>
								<c:if test="${annulproccomplexeent}">
									<li><fmt:message key="label.action.annulation.processus.complexes"/>
										<ul>
											<authz:authorize access="hasAnyRole('FAILLITE_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/annulation/faillite/list.do"/>"><fmt:message key="title.faillite"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('DEMENAGEMENT_SIEGE_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/annulation/demenagement/list.do"/>"><fmt:message key="title.demenagement.siege"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('FIN_ACTIVITE_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/annulation/finactivite/list.do"/>"><fmt:message key="title.fin.activite"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('FUSION_ENTREPRISES')">
												<li><a href="<c:url value="/processuscomplexe/annulation/fusion/list.do"/>"><fmt:message key="title.fusion.entreprises"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('SCISSION_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/annulation/scission/list.do"/>"><fmt:message key="title.scission.entreprise"/></a></li>
											</authz:authorize>
											<authz:authorize access="hasAnyRole('TRANSFERT_PATRIMOINE_ENTREPRISE')">
												<li><a href="<c:url value="/processuscomplexe/annulation/transfertpatrimoine/list.do"/>"><fmt:message key="title.transfert.patrimoine"/></a></li>
											</authz:authorize>
										</ul>
									</li>
								</c:if>
							</ul>
						</li>
					</c:if>
					
					<authz:authorize access="hasAnyRole('EVEN', 'EVEN_PM', 'SUIVI_IMPORT_RF', 'SUIVI_ANNONCES_IDE')">
					<li><fmt:message key="title.evenements" />
						<ul>
							<authz:authorize access="hasAnyRole('EVEN')">
								<li><a href="<c:url value='/evenement/regpp/list.do'/>"><fmt:message key="title.evenements.regpp"/></a></li>
								<li><a href="<c:url value='/evenement/ech/list.do'/>"><fmt:message key="title.evenements.ech"/></a></li>
							</authz:authorize>
							<authz:authorize access="hasAnyRole('EVEN_PM')">
								<li><a href="<c:url value='/evenement/organisation/list.do'/>"><fmt:message key="title.evenements.organisation"/></a></li>
							</authz:authorize>
							<authz:authorize access="hasAnyRole('EVEN')">
								<unireg:ifReqDes>
									<li><a href="<c:url value='/evenement/reqdes/list.do'/>"><fmt:message key="title.evenements.reqdes"/></a></li>
								</unireg:ifReqDes>
							</authz:authorize>
							<authz:authorize access="hasAnyRole('SUIVI_IMPORT_RF')">
								<li><a href="<c:url value='/registrefoncier/import/list.do'/>"><fmt:message key="title.evenements.registrefoncier"/></a></li>
							</authz:authorize>
							<authz:authorize access="hasAnyRole('SUIVI_ANNONCES_IDE')">
								<li><a href="<c:url value='/annonceIDE/find.do'/>"><fmt:message key="title.suivi.demandes.menu"/></a></li>
							</authz:authorize>
						</ul>
					</li>
					</authz:authorize>

					<authz:authorize access="hasAnyRole('VISU_ALL')">
					<authz:authorize access="hasAnyRole('MODIF_VD_ORD', 'MODIF_VD_SOURC', 'MODIF_HC_HS', 'MODIF_HAB_DEBPUR', 'MODIF_NONHAB_DEBPUR', 'MODIF_PM', 'DI_EMIS_PP', 'DI_EMIS_PM', 'QSNC_EMISSION')">
						<li><a href="<c:url value='/tache/list.do'/>"><fmt:message key="title.taches" /></a></li>
					</authz:authorize>
					</authz:authorize>
					<authz:authorize access="hasAnyRole('FORM_OUV_DOSS')">
						<li><a href="<c:url value='/tache/list-nouveau-dossier.do'/>"><fmt:message key="title.nouveaux.dossiers" /></a></li>
					</authz:authorize>

					<authz:authorize access="hasAnyRole('MVT_DOSSIER_MASSE')">
					    <li><fmt:message key="title.mouvements.dossiers.masse"/>
					        <ul>
					        <li><a href="<c:url value='/mouvement/masse/consulter.do'/>"><fmt:message key="title.mouvements.dossiers.masse.consulter"/></a></li>
					        <li><a href="<c:url value='/mouvement/masse/pour-traitement.do'/>"><fmt:message key="title.mouvements.dossiers.masse.traiter"/></a></li>
					        <li><a href="<c:url value='/mouvement/bordereau/a-imprimer.do'/>"><fmt:message key="title.mouvements.dossiers.masse.imprimer.bordereaux"/></a></li>
					        <li><a href="<c:url value='/mouvement/bordereau/reception.do'/>"><fmt:message key="title.mouvements.dossiers.masse.receptionner"/></a></li>
					        </ul>
					    </li>
					</authz:authorize>
					
					<authz:authorize access="hasAnyRole('PARAM_APP', 'PARAM_PERIODE')">
						<li><fmt:message key="label.action.parametrage" />
							<ul>
							<authz:authorize access="hasAnyRole('PARAM_APP')">
								<li><a href="<c:url value='/param/app/list.do'/>"><fmt:message key="label.action.param.application" /></a></li>
							</authz:authorize>
							<authz:authorize access="hasAnyRole('PARAM_PERIODE')">
								<li><a href="<c:url value='/param/periode/list.do'/>"><fmt:message key="label.action.param.periode" /></a></li>
							</authz:authorize>
							</ul>
						</li>
					</authz:authorize>
					
					<authz:authorize access="hasAnyRole('SEC_DOS_LEC', 'SEC_DOS_ECR')">
						<li><fmt:message key="label.action.acces" />
							<ul>
								<li><a href="<c:url value='/acces/par-dossier.do'/>"><fmt:message key="label.action.par.dossier" /></a></li>
								<li><a href="<c:url value='/acces/par-utilisateur.do'/>"><fmt:message key="label.action.par.utilisateur" /></a></li>
								<authz:authorize access="hasAnyRole('SEC_DOS_ECR')">
									<li><a href="<c:url value='/acces/copie-transfert.do'/>"><fmt:message key="label.action.copie.transfert" /></a></li>
								</authz:authorize>
							</ul>
						</li>
					</authz:authorize>
					
					<authz:authorize access="hasAnyRole('MW_IDENT_CTB_VISU', 'MW_IDENT_CTB_CELLULE_BO', 'MW_IDENT_CTB_GEST_BO', 'MW_IDENT_CTB_ADMIN', 'NCS_IDENT_CTB_CELLULE_BO', 'LISTE_IS_IDENT_CTB_CELLULE_BO', 'RAPPROCHEMENT_RF_IDENTIFICATION_CTB')">
						<li><fmt:message key="label.identification.ctb" />
							<ul>
								<li><a href="<c:url value='/identification/gestion-messages/nav-listEnCours.do?wipeCriteria=yes'/>"><fmt:message key="label.demande.en.cours" /></a></li>
								<authz:authorize access="hasAnyRole('MW_IDENT_CTB_ADMIN', 'MW_IDENT_CTB_GEST_BO')">
									<li><a href="<c:url value='/identification/gestion-messages/nav-listSuspendu.do?wipeCriteria=yes'/>"><fmt:message key="label.demande.suspendu" /></a></li>
								</authz:authorize>
								<authz:authorize access="hasAnyRole('MW_IDENT_CTB_ADMIN')">
									<li><a href="<c:url value='/identification/tableau-bord/stats.do'/>"><fmt:message key="label.tableau.bord" /></a></li>
								</authz:authorize>
							     <authz:authorize access="hasAnyRole('MW_IDENT_CTB_VISU', 'MW_IDENT_CTB_CELLULE_BO', 'MW_IDENT_CTB_GEST_BO', 'MW_IDENT_CTB_ADMIN', 'NCS_IDENT_CTB_CELLULE_BO', 'LISTE_IS_IDENT_CTB_CELLULE_BO', 'RAPPROCHEMENT_RF_IDENTIFICATION_CTB')">
									<li><a href="<c:url value='/identification/gestion-messages/nav-listTraite.do?wipeCriteria=yes'/>"><fmt:message key="label.demande.archives" /></a></li>
								</authz:authorize>
							</ul>
						</li>
					</authz:authorize>

					<unireg:ifEfacture>
	                    <authz:authorize access="hasAnyRole('GEST_QUIT_EFACTURE')">
	                        <li><fmt:message key="label.efacture"/>
	                            <ul>
	                                <li><a href="<c:url value='/efacture/quittancement/show.do'/>"><fmt:message key="label.efacture.quittancement"/></a></li>
	                            </ul>
	                        </li>
	                    </authz:authorize>
					</unireg:ifEfacture>

					<authz:authorize access="hasAnyRole('GEST_QUIT_LETTRE_BIENVENUE')">
						<li><fmt:message key="label.autre.document.fiscal.lettre.bienvenue"/>
							<ul>
								<li><a href="<c:url value='/autresdocs/lettrebienvenue/quittancement/show.do'/>"><fmt:message key="label.autre.document.fiscal.lettre.bienvenue.quittancement"/></a></li>
							</ul>

						</li>
					</authz:authorize>

					<authz:authorize access="hasAnyRole('GEST_FRACTIONS_COMMUNE_RF', 'ELECTION_PRINCIPAL_COMMUNAUTE_RF')">
						<li><fmt:message key="label.registre.foncier"/>
							<ul>
								<authz:authorize access="hasAnyRole('GEST_FRACTIONS_COMMUNE_RF')">
								<li><a href="<c:url value='/registrefoncier/situation/surcharge/list.do'/>"><fmt:message key="label.fraction.communes"/></a></li>
								</authz:authorize>
								<authz:authorize access="hasAnyRole('ELECTION_PRINCIPAL_COMMUNAUTE_RF')">
								<li><a href="<c:url value='/registrefoncier/communaute/searchTiers.do'/>"><fmt:message key="label.election.principal.communes"/></a></li>
								</authz:authorize>
							</ul>
						</li>
					</authz:authorize>

					<authz:authorize access="hasAnyRole('TESTER', 'ADMIN')">
						<li><fmt:message key="label.action.admin" />
							<ul>
								<li><a href="<c:url value='/admin/indexation/show.do'/>"><fmt:message key="title.indexation" /></a></li>
								<li><a href="<c:url value='/admin/batch.do'/>"><fmt:message key="title.batchs" /></a></li>
								<li><a href="<c:url value='/admin/jms/show.do'/>"><fmt:message key="title.jms" /></a></li>
								<li><a href="<c:url value='/admin/audit.do'/>"><fmt:message key="title.admin.audit" /></a></li>
							</ul>
						</li>

						<unireg:testMode>
						<li><fmt:message key="label.action.testing" />
							<ul>
								<unireg:ifEnv devel="true">
									<li><a href="<c:url value='/admin/tiersImport/list.do'/>"><fmt:message key="title.charger.tiers" /></a></li>
								</unireg:ifEnv>
								<li><a href="<c:url value='/admin/dbpreview.do'/>"><fmt:message key="title.preview.tiers" /></a></li>
							</ul>
						</li>
						</unireg:testMode>
						
						<!-- on affiche le lien vers la page d'info (strictement read-only) dans tous les cas -->
						<li><a href="<c:url value='/admin/status.do'/>"><fmt:message key="title.info.link"/></a></li>
					</authz:authorize>

				</ul>
				<authz:authorize access="hasAnyRole('VISU_ALL')">
					<authz:authorize access="hasAnyRole('MODIF_VD_ORD', 'MODIF_VD_SOURC', 'MODIF_HC_HS', 'MODIF_HAB_DEBPUR', 'MODIF_NONHAB_DEBPUR', 'FORM_OUV_DOSS')">

						<div id="postit" class="postit" style="display:none;">
							<table cellpadding="0" class="postit" border="0" cellspacing="0">
								<tbody>
									<tr class="top">
										<td class="iepngfix"></td>
									</tr>
									<tr class="middle">
										<td><span id="postitText"></span></td>
									</tr>
									<tr class="bottom">
										<td class="iepngfix"></td>
									</tr>
								</tbody>
							</table>
						</div>

						<script>
							$(Postit.refresh);
						</script>
					</authz:authorize>
				</authz:authorize>
			</div>

			<div id="header" >
				<span class="departement"><a href="http://www.aci.vd.ch" target="_blank"><fmt:message key="label.aci" /></a></span>
				<unireg:testMode>
					<div style="color:LawnGreen; left:390px; top:20px; position:absolute; font-size:21pt; font-weight:bold; z-index:100">
						<span style="color:white;font-size:21pt;font-weight: bold">(</span>
							<unireg:environnement/>
						<span style="color:white;font-size:21pt;font-weight: bold">)</span>
					</div>
				</unireg:testMode>
				<div class="application iepngfix"></div>
				<%-- le champ d'accès rapide dans l'entête de l'application --%>
				<div class="quicksearch">
					<label for="quicksearch"><fmt:message key="label.acces.rapide"/></label>&nbsp;
					<input id="quicksearch" class="quicksearch" size="15" maxlength="15" type="text"
						onKeyPress="return Quicksearch.onKeyPress(this, event);"
						onfocus="Quicksearch.onFocus(this, '<fmt:message key="label.acces.rapide.invite"/>')"
						onblur="Quicksearch.onBlur(this, '<fmt:message key="label.acces.rapide.invite"/>')"
						value="<fmt:message key="label.acces.rapide.invite"/>"/> 
				</div>
							
			</div>

			<div id="content">
				<div id="outils" class="outils">
					<h3>Outils</h3>
					<tiles:getAsString name='tools' ignore='true'/>
					<ul>
						<li>
						    <unireg:user/>
						</li>
						<tiles:getAsString name='vue' ignore='true'/>
						<li>
							<a href="<c:url value='/'/>" title="Recherche" accesskey="d"><fmt:message key="label.recherche" /></a>
						</li>
						<tiles:getAsString name='fichierAide' ignore='true'/>
						<authz:authorize access="hasAnyRole('VISU_ALL')">
						<li>
                            <%--suppress JspAbsolutePathInspection, HtmlUnknownTarget --%>
                            <a href="/fiscalite/kbaci/advancedSearch.htm" title="Base ACI" accesskey="b"><fmt:message key="label.kbaci" /></a>
						</li>
						</authz:authorize>
						<li>
                            <%--suppress JspAbsolutePathInspection, HtmlUnknownTarget --%>
                            <a href="/iam/accueil/" target="_blank" title="Portail IAM" accesskey="x"><fmt:message key="label.portail.iam" /></a>
						</li>
						<li>
							<a href="<c:url value='/logoutIAM.do'/>" title="Quitter Unireg" accesskey="l"><fmt:message key="label.deconnexion" /></a>
						</li>
					</ul>
				</div>
				<div style="padding: 5px;">
					<div class="empty" style="height: 10px;">&nbsp;</div>
					<br>

					<%-- Message d'avertissement en cas d'offset sur la date courante --%>
					<unireg:dateOffset/>

					<%-- Message flash --%>
					<%--@elvariable id="flash" type="ch.vd.unireg.common.FlashMessage"--%>
					<c:if test="${flash != null && flash.active}">
						<div id="flashdisplay" class="<c:out value='${flash.displayClass}'/>"><c:out value="${flash.messageForDisplay}"/></div>
						<c:if test="${flash.timeout > 0}">
							<script type="text/javascript">
								$('#flashdisplay').delay(<c:out value="${flash.timeout}"/>).fadeOut('slow');
							</script>
						</c:if>
					</c:if>

					<%@ include file="/WEB-INF/jsp/include/ie6-warning.jsp" %>

					<h1><tiles:getAsString name='title' ignore='true'/><tiles:getAsString name="displayedTitleSuffix" ignore="true"/></h1>
					<div id="globalErrors">
						<c:set var="globalErrorCount" value="0"/>
						<spring:hasBindErrors name="command">
							<c:if test="${errors.globalErrorCount > 0}">
								<c:forEach var="error" items="${errors.globalErrors}">
									<c:if test="${unireg:startsWith(error.code, 'global.error')}">
										<c:set var="globalErrorCount" value="${globalErrorCount+1}"/>
									</c:if>
								</c:forEach>
							</c:if>
						</spring:hasBindErrors>
						<%--@elvariable id="actionErrors" type="ch.vd.unireg.common.ActionMessageList"--%>
						<c:if test="${actionErrors != null && not empty actionErrors}">
							<c:set var="globalErrorCount" value="${globalErrorCount + fn:length(actionErrors)}"/>
						</c:if>
						<c:if test="${globalErrorCount > 0}">
							<table class="action_error" cellspacing="0" cellpadding="0" border="0">
								<tr><td class="heading"><fmt:message key="label.action.problemes.detectes"/></td></tr>
								<tr><td class="details"><ul>
									<spring:hasBindErrors name="command">
										<c:forEach var="error" items="${errors.globalErrors}">
											<c:if test="${unireg:startsWith(error.code, 'global.error')}">
												<li class="err"><spring:message message="${error}" /></li>
											</c:if>
										</c:forEach>
									</spring:hasBindErrors>
									<c:if test="${actionErrors != null}">
										<c:forEach var="error" items="${actionErrors}">
											<li class="<c:out value='${error.displayClass}'/>"><c:out value="${error.message}"/></li>
										</c:forEach>
										<c:set target="${actionErrors}" property="active" value="false"/>
									</c:if>
								</ul></td></tr>
							</table>
						</c:if>
					</div>
					<div class="workaround_IE_bug">
						<tiles:getAsString name='body' />
					</div>
				</div>
			</div>

			<div id="footer">
				<strong>Version <fmt:message key="version" /></strong>&nbsp;&nbsp;&nbsp;(Build: <fmt:message key="buildtime"/> / <fmt:message key="gitCommitId"/>)
				&nbsp;&nbsp;&nbsp;<strong><unireg:environnement/></strong>
				&nbsp;&nbsp;&nbsp;<small><a href="<c:url value="/about.do"/>">A propos</a></small>
				<br/>
				<strong>Navigateur&nbsp;:</strong> <div id="appVersion" style="display:inline-block">?</div>
				<script type="text/javascript">
					$('#appVersion').html(navigator.userAgent);
				</script>
				<br/>&nbsp;<br/>
			</div>
			
			<%@ include file="/WEB-INF/jsp/include/tabs-workaround.jsp" %>

			<%-- Téléchargement décalé de document --%>
			<%--@elvariable id="delayedDownloadId" type="java.util.UUID"--%>
			<c:if test="${delayedDownloadId != null}">
				<iframe id="delayedDownloadIFrame" style="display:none;" src="<c:url value='/delayed-download.do?url_memorize=false&id=${delayedDownloadId}'/>"></iframe>
			</c:if>

	</body>

</html>

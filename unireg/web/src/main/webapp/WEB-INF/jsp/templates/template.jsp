<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<script type="text/javascript" language="Javascript1.3">
function ouvrirAide(url) {
 	window.open(url, "aide", "top=100, left=750, screenY=50, screenX=100, width=500, height=800, directories=no, location=no, menubar=no, status=no, toolbar=no, resizable=yes");
}
</script>

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<tiles:getAsString name='refresh' ignore='true'/>
		<link rel="SHORTCUT ICON" href="<c:url value="/images/favicon.ico"/>">

		<link media="screen" href="<c:url value="/css/x/jquery-ui.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/screen-all.css"/>" rel="stylesheet" type="text/css">

		<%@ include file="/WEB-INF/jsp/include/png-workaround.jsp" %>
		<%@ include file="/WEB-INF/jsp/include/fieldsets-workaround.jsp" %>

		<link media="print" href="<c:url value="/css/print/print-all.css"/>" rel="stylesheet" type="text/css">

		<script type="text/javascript" language="javascript">
			function getContextPath() {
			  return '<c:url value="/"/>';
			}
		</script>

		<script type="text/javascript" language="Javascript" src="<c:url value="/js/unireg.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery-all.js"/>"></script>

		<title><tiles:getAsString name='title' ignore='false'/></title>
		<tiles:getAsString name='head' ignore='true'/>
	</head>
	<body>

			<div id="sommaire">
				<div style="position: absolute;top: 5px;left: 5px;"><div id="loadingImage" style="visibility: hidden;"><img id="loadingImageSign" src="<c:url value="/images/loading.gif"/>" /></div></div>
				<div class="canton">
					<a href="http://www.vd.ch" target="_blank">
						<span class="label"><fmt:message key="label.canton.vaud" /></span>
					</a>
				</div>
				<h3>Sommaire&nbsp;<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/sommaire-glossaire.pdf'/>');" title="AccessKey: a" accesskey="e">?</a>
				</h3>
				<tiles:getAsString name='links' ignore='true'/>
				<ul>
					<li><a href="<c:url value='/tiers/list.do'/>"><fmt:message key="title.rechercher" /></a></li>

					<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
						<li>
							<a href="<c:url value='/admin/inbox/show.do'/>">
								<span id="inboxSize"><fmt:message key="title.inbox"/></span>
							</a>
						</li>

						<script>
							// appels ajax pour mettre-à-jour le nombre d''éléments non-lus de l''inbox
							var requestInboxSizeDone = true;

							function refreshInboxSize() {
								if (!requestInboxSizeDone) {
									return;
								}
								requestInboxSizeDone = false;
								$.get(getContextPath() + "/admin/inbox/unreadSize.do?" + new Date().getTime(), function(unreadSize) {
									if (unreadSize > 0) {
										$('#inboxSize').text('<fmt:message key="title.inbox"/> (' + unread + ')');
										$('#inboxSize').attr('style', 'font-weight: bold');
									}
									else {
										$('#inboxSize').text('<fmt:message key="title.inbox"/>');
										$('#inboxSize').attr('style', '');
									}
									onReceivedInboxSize();
								});
							}

							// dès l''affichage de la page
							refreshInboxSize();

							function onReceivedInboxSize() {
								requestInboxSizeDone = true;
							}
						</script>

					</authz:authorize>

					<authz:authorize ifAnyGranted="ROLE_CREATE_NONHAB, ROLE_CREATE_AC, ROLE_MODIF_VD_ORD, ROLE_MODIF_VD_SOURC, ROLE_MODIF_HC_HS, ROLE_MODIF_HAB_DEBPUR, ROLE_MODIF_NONHAB_DEBPUR, ROLE_MODIF_NONHAB_INACTIF">
						<li><fmt:message key="label.action.creation" />
							<ul>
								<authz:authorize ifAnyGranted="ROLE_CREATE_NONHAB">
									<li><a href="<c:url value='/tiers/create.do'/>?nature=NonHabitant"><fmt:message key="title.inconnu.controle.habitants" /></a></li>
								</authz:authorize>
								<authz:authorize ifAnyGranted="ROLE_CREATE_AC">
									<li><a href="<c:url value='/tiers/create.do'/>?nature=AutreCommunaute"><fmt:message key="title.inconnu.pm" /></a></li>
								</authz:authorize>
								<c:if test="${false}"><!-- La fusion est désactivée -->
								<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD, ROLE_MODIF_VD_SOURC, ROLE_MODIF_HC_HS, ROLE_MODIF_HAB_DEBPUR, ROLE_MODIF_NONHAB_DEBPUR, ROLE_MODIF_NONHAB_INACTIF">
									<li><a href="<c:url value='/fusion/list-non-habitant.do'/>"><fmt:message key="title.fusion" /></a></li>
								</authz:authorize>
								</c:if>
								<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD, ROLE_MODIF_VD_SOURC, ROLE_MODIF_HC_HS, ROLE_MODIF_HAB_DEBPUR, ROLE_MODIF_NONHAB_DEBPUR">
									<li><a href="<c:url value='/couple/create.do'/>"><fmt:message key="title.couple" /></a></li>
									<li><a href="<c:url value='/separation/list.do'/>"><fmt:message key="title.separation" /></a></li>
									<li><a href="<c:url value='/deces/list.do'/>"><fmt:message key="title.deces" /></a></li>
									<authz:authorize ifAnyGranted="ROLE_ANNUL_TIERS">
										<li><a href="<c:url value='/activation/list.do?activation=reactivation'/>"><fmt:message key="title.reactivation.tiers" /></a></li>
									</authz:authorize>
								</authz:authorize>
							</ul>
						</li>
						<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD, ROLE_MODIF_VD_SOURC, ROLE_MODIF_HC_HS, ROLE_MODIF_HAB_DEBPUR, ROLE_MODIF_NONHAB_DEBPUR">
							<li><fmt:message key="label.action.annulation" />
								<ul>
									<li><a href="<c:url value='/annulation/couple/list.do'/>"><fmt:message key="title.couple" /></a></li>
									<li><a href="<c:url value='/annulation/separation/list.do'/>"><fmt:message key="title.separation" /></a></li>
									<li><a href="<c:url value='/annulation/deces/list.do'/>"><fmt:message key="title.deces" /></a></li>
									<authz:authorize ifAnyGranted="ROLE_ANNUL_TIERS">
										<li><a href="<c:url value='/activation/list.do?activation=annulation'/>"><fmt:message key="title.tiers" /></a></li>
									</authz:authorize>
								</ul>
							</li>
						</authz:authorize>
					</authz:authorize>
					<authz:authorize ifAnyGranted="ROLE_LR">
						<li><a href="<c:url value='/lr/list.do'/>"><fmt:message key="title.lr" /></a></li>
					</authz:authorize>
					<authz:authorize ifAnyGranted="ROLE_EVEN">
					<li><fmt:message key="title.evenements" />
						<ul>
						<li><a href="<c:url value='/evenement/list.do'/>"><fmt:message key="title.evenements.pp" /></a></li>
						<li><a href="<c:url value='/evenement/list.do'/>"><fmt:message key="title.evenements.ech" /></a></li>
						</ul>
					</li>
					</authz:authorize>

					<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD, ROLE_MODIF_VD_SOURC, ROLE_MODIF_HC_HS, ROLE_MODIF_HAB_DEBPUR, ROLE_MODIF_NONHAB_DEBPUR">
						<li><a href="<c:url value='/tache/list.do'/>"><fmt:message key="title.taches" /></a></li>
					</authz:authorize>
					</authz:authorize>
					<authz:authorize ifAnyGranted="ROLE_FORM_OUV_DOSS">
						<li><a href="<c:url value='/tache/list-nouveau-dossier.do'/>"><fmt:message key="title.nouveaux.dossiers" /></a></li>
					</authz:authorize>

					<authz:authorize ifAnyGranted="ROLE_MVT_DOSSIER_MASSE">
					    <li><fmt:message key="title.mouvements.dossiers.masse"/>
					        <ul>
					        <li><a href="<c:url value='/mouvement/masse/consulter.do'/>"><fmt:message key="title.mouvements.dossiers.masse.consulter"/></a></li>
					        <li><a href="<c:url value='/mouvement/masse/pour-traitement.do'/>"><fmt:message key="title.mouvements.dossiers.masse.traiter"/></a></li>
					        <li><a href="<c:url value='/mouvement/bordereau/a-imprimer.do'/>"><fmt:message key="title.mouvements.dossiers.masse.imprimer.bordereaux"/></a></li>
					        <li><a href="<c:url value='/mouvement/bordereau/reception.do'/>"><fmt:message key="title.mouvements.dossiers.masse.receptionner"/></a></li>
					        </ul>
					    </li>
					</authz:authorize>
					
					<authz:authorize ifAnyGranted="ROLE_PARAM_APP, ROLE_PARAM_PERIODE">
						<li><fmt:message key="label.action.parametrage" />
							<ul>
							<authz:authorize ifAnyGranted="ROLE_PARAM_APP">
								<li><a href="<c:url value='/param/application.do'/>"><fmt:message key="label.action.param.application" /></a></li>
							</authz:authorize>
							<authz:authorize ifAnyGranted="ROLE_PARAM_PERIODE">
								<li><a href="<c:url value='/param/periode.do'/>"><fmt:message key="label.action.param.periode" /></a></li>
							</authz:authorize>
							</ul>
						</li>
					</authz:authorize>
					
					<authz:authorize ifAnyGranted="ROLE_SEC_DOS_LEC, ROLE_SEC_DOS_ECR">
						<li><fmt:message key="label.action.acces" />
							<ul>
								<li><a href="<c:url value='/acces/list-pp.do'/>"><fmt:message key="label.action.par.dossier" /></a></li>
								<li><a href="<c:url value='/acces/select-utilisateur.do'/>"><fmt:message key="label.action.par.utilisateur" /></a></li>
								<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
									<li><a href="<c:url value='/acces/select-utilisateurs.do'/>"><fmt:message key="label.action.copie.transfert" /></a></li>
								</authz:authorize>
							</ul>
						</li>
					</authz:authorize>
					
					<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_VISU,ROLE_MW_IDENT_CTB_CELLULE_BO,ROLE_MW_IDENT_CTB_GEST_BO,ROLE_MW_IDENT_CTB_ADMIN,ROLE_NCS_IDENT_CTB_CELLULE_BO">
						<li><fmt:message key="label.identification.ctb" />
							<ul>
								<li><a href="<c:url value='/identification/gestion-messages/listEnCours.do'/>"><fmt:message key="label.demande.en.cours" /></a></li>
								<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_ADMIN">
									<li><a href="<c:url value='/identification/tableau-bord/stats.do'/>"><fmt:message key="label.tableau.bord" /></a></li>									
								</authz:authorize>
							     <authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_VISU,ROLE_MW_IDENT_CTB_CELLULE_BO,ROLE_MW_IDENT_CTB_GEST_BO,ROLE_MW_IDENT_CTB_ADMIN,ROLE_NCS_IDENT_CTB_CELLULE_BO">
									<li><a href="<c:url value='/identification/gestion-messages/listTraite.do'/>"><fmt:message key="label.demande.archives" /></a></li>
								</authz:authorize>
							</ul>
						</li>
					</authz:authorize>
					
					<authz:authorize ifAnyGranted="ROLE_TESTER, ROLE_ADMIN">
						<li><fmt:message key="label.action.admin" />
							<ul>
								<li><a href="<c:url value='/admin/indexation.do'/>"><fmt:message key="title.indexation" /></a></li>
								<li><a href="<c:url value='/admin/batch.do'/>"><fmt:message key="title.batchs" /></a></li>
								<li><a href="<c:url value='/admin/performance.do'/>"><fmt:message key="title.statistique.performances" /></a></li>
								<li><a href="<c:url value='/admin/audit.do'/>"><fmt:message key="title.admin.audit" /></a></li>
							</ul>
						</li>

						<unireg:testMode>
						<li><fmt:message key="label.action.testing" />
							<ul>
								<unireg:ifEnv devel="true" standalone="true">
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
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD, ROLE_MODIF_VD_SOURC, ROLE_MODIF_HC_HS, ROLE_MODIF_HAB_DEBPUR, ROLE_MODIF_NONHAB_DEBPUR, ROLE_FORM_OUV_DOSS">

						<div id="postit" class="postit" style="display:none;">
							<table cellpadding="0" class="postit" classname="postit" border="0" cellspacing="0">
								<tbody>
									<tr class="top" classname="top">
										<td class="iepngfix" classname="iepngfix"></td>
									</tr>
									<tr class="middle" classname="middle">
										<td><span id="postitText"></span></td>
									</tr>
									<tr class="bottom" classname="bottom">
										<td class="iepngfix" classname="iepngfix"></td>
									</tr>
								</tbody>
							</table>
						</div>

						<script>
							$(function() {
								Postit.refresh();
							});
						</script>
					</authz:authorize>
				</authz:authorize>
			</div>

			<div id="header" >
				<span class="departement"><a href="http://www.aci.vd.ch" target="_blank"><fmt:message key="label.aci" /></a></span>
				<unireg:testMode>
					<div style="color:LawnGreen; left:360px; top:20px; position:absolute; font-size:21pt; font-weight:bold; z-index:100">
						<span style="color:white;font-size:21pt;font-weight: bold">(</span>
							<unireg:environnement/>
						<span style="color:white;font-size:21pt;font-weight: bold">)</span>
					</div>
				</unireg:testMode>
				<div class="application iepngfix"></div>
				<%-- le champ d'accès rapide dans l'entête de l'application --%>
				<div class="quicksearch">
					<fmt:message key="label.acces.rapide"/>&nbsp;
					<input class="quicksearch" size="15" type="text" 
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
						<unireg:user></unireg:user>
						</li>
						<tiles:getAsString name='vue' ignore='true'/>
						<li>
							<a href="<c:url value='/'/>" title="Recherche" accesskey="d"><fmt:message key="label.recherche" /></a>
						</li>
						<li>
							<tiles:getAsString name='fichierAide' ignore='true'/>
						</li>
						<li>
							<a href="/fiscalite/kbaci/advancedSearch.htm" title="Base ACI" accesskey="b"><fmt:message key="label.kbaci" /></a>
						</li>
						<li>
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
					<c:if test="${flash != null && flash.active}">
						<div id="flashdisplay" class="<c:out value='${flash.displayClass}'/>"><c:out value="${flash.messageForDisplay}"/></div>
						<c:if test="${flash.timeout > 0}">
							<script type="text/javascript">
								$('#flashdisplay').delay(<c:out value="${flash.timeout}"/>).fadeOut('slow');
							</script>
						</c:if>
					</c:if>

					<h1><tiles:getAsString name='title' ignore='true'/></h1>
					<div id="globalErrors">
						<spring:hasBindErrors name="command">
							<c:set var="globalErrorCount" value="0"/>
							<c:if test="${errors.globalErrorCount > 0}">
								<c:forEach var="error" items="${errors.globalErrors}">
									<c:if test="${unireg:startsWith(error.code, 'global.error')}">
										<c:set var="globalErrorCount" value="${globalErrorCount+1}"/>
									</c:if>
								</c:forEach>
							</c:if>
							<c:if test="${globalErrorCount > 0}">
								<table class="action_error" cellspacing="0" cellpadding="0" border="0">
									<tr><td class="heading"><fmt:message key="label.action.problemes.detectes"/></td></tr>
									<tr><td class="details"><ul>
									<c:forEach var="error" items="${errors.globalErrors}">
										<c:if test="${unireg:startsWith(error.code, 'global.error')}">
											<li class="err"><spring:message message="${error}" /></li>
										</c:if>
									</c:forEach>
									</ul></td></tr>
								</table>
							</c:if>
						</spring:hasBindErrors>
					</div>
					<div class="workaround_IE6_bug">
						<tiles:getAsString name='body' />
					</div>
				</div>
			</div>

			<div id="footer">
				<b><fmt:message key="version" /></b>&nbsp;&nbsp;&nbsp;(Build: <fmt:message key="buildtime"/>)
				&nbsp;&nbsp;&nbsp;<b><unireg:environnement/></b>
			</div>
			
			<%@ include file="/WEB-INF/jsp/include/tabs-workaround.jsp" %>

	</body>

</html>

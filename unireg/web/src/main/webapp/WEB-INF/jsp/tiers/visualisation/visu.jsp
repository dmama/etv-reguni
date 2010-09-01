<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head"><script type="text/javascript" src="<c:url value="/js/tiers.js"/>"></script></tiles:put>
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
		<table class="warnings" cellspacing="0" cellpadding="0" border="0">
			<tr><td class="heading"><fmt:message key="label.action.avertissements"/></td></tr>
			<tr id="val_errors"><td class="details"><ul>
			<c:forEach var="warn" items="${warnings}">
				<li class="warn"><c:out value="${warn}"/></li>
			</c:forEach>
			</ul></td></tr>
		</table>
	</c:if>

	<c:if test="${command.tiers != null}">

		<authz:authorize ifAnyGranted="ROLE_SUPERGRA">
			<div style="float: right; display: inline; margin-top: -1.5em;">
				<a href="<c:url value="/supergra/entity.do?id=${command.tiersGeneral.numero}&class=Tiers"/>">Edition de ce tiers en mode SuperGra</a>
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
		<div id="tabs">
			<ul id="tiersTabs">
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<li id="fiscalTab">
						<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.fiscal" /></a>
					</li>
					<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
						<li id="civilTab">
							<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.civil" /></a>
						</li>
					</c:if>
				</authz:authorize>
				<li id="adressesTab">
					<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.adresse" /></a>
				</li>
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<li id="complementsTab">
						<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.complements" /></a>
					</li>
					<c:if test="${command.natureTiers == 'DebiteurPrestationImposable'}">
						<li id="rapportsPrestationTab">
							<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.dossiers.apparentes" /></a>
						</li>
						<li id="lrTab">
							<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.lr" /></a>
						</li>
					</c:if>
				</authz:authorize>
				<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
					<li id="dossiersApparentesTab">
						<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.dossiers.apparentes" /></a>
					</li>
					<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<li id="diTab">
						<a href="#" onclick="javascript:Tabulation.show(this);""><fmt:message key="label.di" /></a>
					</li>
					<li id="mouvementTab">
						<a href="#" onclick="javascript:Tabulation.show(this);""><fmt:message key="label.mouvement" /></a>
					</li>
					</authz:authorize>
				</c:if>
				<li id="remarqueTab">
					<a id="remarqueTabAnchor" href="#" onclick="javascript:Tabulation.show(this);""><fmt:message key="label.remarques" /></a>
				</li>
			</ul>
		</div>
		<!-- Fin onglets -->
		<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
			<div id="tabContent_fiscalTab" class="situation_fiscale" style="display: none;">
				<jsp:include page="fiscal/fiscal.jsp"/>
			</div>
			<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
				<div id="tabContent_civilTab" class="editTiers" style="display: none;">
					<jsp:include page="civil/civil.jsp"/>
				</div>
			</c:if>
		</authz:authorize>
		<div id="tabContent_adressesTab" class="adresses" style="display: none;">
			<jsp:include page="adresse/adresse.jsp"/>
		</div>
		<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
			<div id="tabContent_complementsTab" class="editTiers" style="display: none;">
				<jsp:include page="complement.jsp"/>
			</div>
			<c:if test="${command.natureTiers == 'DebiteurPrestationImposable'}">
				<div id="tabContent_rapportsPrestationTab" class="visuTiers" style="display: none;">
					<jsp:include page="rapports-prestation.jsp"/>
				</div>
				<div id="tabContent_lrTab" class="visuTiers">
					<jsp:include page="lr/lrs.jsp"/>
				</div>
			</c:if>
		</authz:authorize>
			<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
				
				<div id="tabContent_dossiersApparentesTab" class="visuTiers" style="display: none;">
					<jsp:include page="dossiers-apparentes.jsp"/>
					<jsp:include page="debiteur.jsp"/>
				</div>
				<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
					<div id="tabContent_diTab" class="visuTiers" style="display: none;">
						<jsp:include page="di/dis.jsp"/>
					</div>
				</authz:authorize>
			</c:if>
		<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
			<div id="tabContent_mouvementTab" class="visuTiers" style="display: none;">
				<jsp:include page="mouvement/mouvements.jsp"/>
			</div>
		</authz:authorize>

		<div id="tabContent_remarqueTab" class="visuTiers" style="display:none">
				<jsp:include page="../common/remarque/remarques.jsp">
					<jsp:param name="tiersId" value="${command.tiersGeneral.numero}" />
				</jsp:include>
		</div>
		
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
			Tabulation.attachObserver("change", Tab_Change);
			var tabulationInitalized = false;						
			var onglet = request.getParameter("onglet");
			if ( onglet) {
				Tabulation.show( onglet);
			} else {
				Tabulation.restoreCurrentTabulation("tiersTabs");
			}
			
			function Tab_Change( selectedTab) {
				if( selectedTab) {
					tabulationInitalized = true;
				}
				if (!tabulationInitalized) {					
					Tabulation.showFirst( "tiersTabs");					
				}
			} 

			function Page_AnnulerTiers(ev) {
				if(!confirm('Voulez-vous vraiment annuler ce tiers ?'))
					return Event.stop(ev);
				return true;
		 	}
		 	
		 	function showPrintView() {
		 		Tabulation.showAll('tiersTabs');
		 		E$("tabs").style.display="none";
		 		E$("tabnav-disable").style.display="none";
		 		E$("tabnav-enable").style.display="";
		 	}
		 	
		 	function showScreenView() {
		 		Tabulation.showFirst('tiersTabs');
		 		E$("tabs").style.display="";
		 		E$("tabnav-disable").style.display="";
		 		E$("tabnav-enable").style.display="none";
		 	}
	</script>
	<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
		<script type="text/javascript" language="Javascript1.3">
				toggleRowsIsHisto('forFiscal', 'isForHisto', 6);
				toggleRowsIsHisto('situationFamille','isSFHisto', 5);
				toggleRowsIsHisto('dossierApparente','isRapportHisto', 2);
		</script>
	</c:if>
	<c:if test="${command.natureTiers == 'DebiteurPrestationImposable'}">
		<script type="text/javascript" language="Javascript1.3">
			toggleRowsIsHisto('forFiscal', 'isForDebHisto', 2);
			toggleRowsIsHistoPeriodicite('periodicite','isPeriodiciteHisto', 2,3);
			toggleRowsIsActif('contribuableAssocie','isCtbAssoHisto', 0);
		</script>
	</c:if>
		
	</tiles:put>
	
</tiles:insert>

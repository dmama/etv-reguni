<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="id" value="${param.id}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<c:if test="${command.tiers != null}">
		<tiles:put name="title">
			<c:choose>
				<c:when test="${command.tiers.numero != null}">
					<fmt:message key="title.edition.tiers" />
				</c:when>
				<c:otherwise>
					<c:if test="${command.natureTiers == 'NonHabitant'}">
						<fmt:message key="title.creation.pp" />
					</c:if>
					<c:if test="${command.natureTiers == 'AutreCommunaute'}">
						<fmt:message key="title.creation.pm" />
					</c:if>
					<c:if test="${command.natureTiers == 'DebiteurPrestationImposable'}">
						<fmt:message key="title.creation.dpi" />
					</c:if>
				</c:otherwise>
			</c:choose>
		</tiles:put>
		
		<c:if test="${command.natureTiers == 'NonHabitant'}">
			<tiles:put name="fichierAide">
				<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/creation-inconnuCdH.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
			</tiles:put>
		</c:if>
		<c:if test="${command.natureTiers == 'AutreCommunaute'}">

		</c:if>
		<c:if test="${command.natureTiers == 'DebiteurPrestationImposable'}">

		</c:if>

	</c:if>
	<tiles:put name="vue">
		<li>
			<a href="javascript:Tabulation.showFirst();"> <span class="form-friendly" style="display: none;" id="tabnav-enable"><fmt:message key="label.vue.ecran" /></span></a>
			<a href="javascript:Tabulation.showAll('tiersTabs');"> <span class="printer-friendly" style="display: block;" id="tabnav-disable"><fmt:message key="label.vue.imprimable" /></span> </a>
		</li>
	</tiles:put>

	<tiles:put name="body">
		<form:form method="post" id="formEditTiers" name="theForm">
			<input type="hidden"  name="__TARGET__" value="">
			<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
			
			<!-- Debut affichage du tier -->
			<c:if test="${command.tiers != null}">
				<c:set var="ligneTableau" value="${1}" scope="request" />
				<!-- Debut Caracteristiques generales -->
				<c:if test="${command.tiers.numero != null}">
					<jsp:include page="../../general/tiers.jsp">
						<jsp:param name="page" value="edit" />
						<jsp:param name="path" value="tiersGeneral" />		
					</jsp:include>
				</c:if>
				<!-- Fin Caracteristiques generales -->

			<!--onglets-->
			<div id="tabs">
			<ul id="tiersTabs">
				<c:if test="${command.allowedOnglet.FISCAL}">
					<li id="fiscalTab"><a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.fiscal" /></a></li>
				</c:if>
				<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
					<li id="civilTab"><a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.civil" /></a></li>
				</c:if>
				<c:if test="${command.allowedOnglet.ADR}">
					<li id="adressesTab"><a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.adresse" /></a></li>
				</c:if>
				<c:if test="${command.allowedOnglet.CPLT}">
					<li id="complementsTab">
						<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.complements" /></a>
					</li>
				</c:if>
				<c:if test="${command.allowedOnglet.RPT}">
					<li id="rapportsPrestationTab">
						<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.rapports.prestation" /></a>
					</li>
				</c:if>
				<c:if test="${command.allowedOnglet.DOS}">
					<li id="dossiersApparentesTab">
						<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.dossier" /></a>
					</li>
				</c:if>
				<c:if test="${command.allowedOnglet.DBT}">
					<li id="debiteurTab">
						<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.debiteur.is" /></a>
					</li>
				</c:if>
			</ul>
			</div>
			<div><!-- Fin onglets -->
			<c:if test="${command.allowedOnglet.FISCAL}">
				<div id="tabContent_fiscalTab" class="situation_fiscale" style="display: none;">
					<jsp:include page="fiscal/fiscal.jsp" />
				</div>
			</c:if>
			<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
				<div id="tabContent_civilTab" class="visuTiers" style="display: none;">
				<c:choose>
					<c:when test="${!(command.allowedOnglet.CIVIL)}">
						<jsp:include page="../visualisation/civil/civil.jsp" />
					</c:when>
					<c:otherwise>
							<jsp:include page="civil/civil.jsp" />
					</c:otherwise>
				</c:choose>
				</div>
			</c:if>
			<c:if test="${command.allowedOnglet.CPLT}">
				<div id="tabContent_complementsTab" class="editTiers" style="display: none;">
					<jsp:include page="complement/complement.jsp" />
				</div>
			</c:if>
			</div>
			<!-- Debut Boutons -->
			<c:choose>
				<c:when test="${command.tiers.numero != null}">
					<input type="submit" name="retourVisualisation" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:return Page_RetourToVisualisation(event || window.event);" />
				</c:when>
				<c:otherwise>
					<input type="submit" name="retourList"  value="<fmt:message key="label.bouton.retour" />" onClick="javascript:return Page_RetourListe(event || window.event);" />
				</c:otherwise>
			</c:choose>

			<input type="submit" name="save"  value="<fmt:message key="label.bouton.sauver" />"  />			
			<c:if test="${command.tiers.numero != null}">
				<authz:authorize ifAnyGranted="ROLE_ANNUL_TIERS">
					<input type="submit" name="annulerTiers" value="<fmt:message key="label.bouton.annuler.tiers" />" onClick="javascript:return Page_AnnulerTiers(event || window.event);" />
				</authz:authorize>
			</c:if>
			<!-- Fin Boutons -->
		&nbsp;
		</c:if> <!-- Fin visualisation du tiers -->
		
		<c:if test="${command.tiers == null}">
			<tiles:put name="title">
				<fmt:message key="title.edition.tiers" />
			</tiles:put>
			<c:if test="${command.allowed}">
				<span class="error"><fmt:message key="error.tiers.inexistant" /></span>
			</c:if>
			<c:if test="${!command.allowed}">
				<span class="error"><fmt:message key="error.tiers.interdit" /></span>
			</c:if>
		</c:if>
	</form:form>
	<script type="text/javascript" language="Javascript1.3">
			Tabulation.attachObserver("change", Tab_Change);
			var tabulationInitalized = false;						
			var onglet = request.getParameter("onglet");
			if ( onglet) {
				Tabulation.show( onglet);
			} else {
				<c:set var="tabInError" value="false" />
				<spring:hasBindErrors name="command">		
					<c:forEach items="${errors.globalErrors}" var="error">
						<c:if test="${unireg:startsWith(error.code, 'onglet.error')}">
						Tabulation.setTabInError('<spring:message message="${error}"/>');
						Tabulation.show('<spring:message message="${error}"/>');
						<c:set var="tabInError" value="true" />
						</c:if>
					</c:forEach>
				</spring:hasBindErrors>
				<c:if test="${not tabInError}">
				Tabulation.restoreCurrentTabulation("tiersTabs");
				</c:if>
			}

			function Tab_Change( selectedTab) {
				if( selectedTab) {
					tabulationInitalized = true;
				}
				if (!tabulationInitalized) {					
					Tabulation.showFirst( "tiersTabs");					
				}
			} 		

			/**
			 * Initialisation de l'observeur du flag 'modifier'
			 */
			Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver ce tiers ?';
			Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver le tiers ?";

			function Page_RetourListe(ev) {
				if ( Modifier.isModified)
					if (!confirm(Modifier.messageOverConfirmation))
						return Event.stop(ev);
				return true;
			}

			function Page_RetourToVisualisation(ev) {
				if ( Modifier.isModified)
					if(!confirm(Modifier.messageOverConfirmation))
						return Event.stop(ev);
				return true;
			}

			function Page_AnnulerTiers(ev) {
				if(!confirm('Voulez-vous vraiment annuler ce tiers ?'))
					return Event.stop(ev);
				return true;
		 	}
	</script>					
	</tiles:put>
</tiles:insert>

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

	<tiles:put name="body">
		<form:form method="post" id="formEditTiers" name="theForm">
			<input type="hidden"  name="__TARGET__" value="">
			<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
			
			<!-- Debut affichage du tier -->
			<c:if test="${command.tiers != null}">
				<unireg:nextRowClass reset="1"/>
				<!-- Debut Caracteristiques generales -->
				<c:if test="${command.tiers.numero != null}">
					<jsp:include page="../../general/tiers.jsp">
						<jsp:param name="page" value="edit" />
						<jsp:param name="path" value="tiersGeneral" />		
					</jsp:include>
				</c:if>
				<!-- Fin Caracteristiques generales -->

			<!--onglets-->
			<div id="tiersCreationTabs">
				<ul>
					<c:if test="${command.allowedOnglet.FISCAL}">
						<li id="fiscalTab"><a href="#tabContent_fiscalTab"><fmt:message key="label.fiscal" /></a></li>
					</c:if>
					<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
						<li id="civilTab"><a href="#tabContent_civilTab"><fmt:message key="label.civil" /></a></li>
					</c:if>
					<c:if test="${command.allowedOnglet.CPLT}">
						<li id="complementsTab">
							<a href="#tabContent_complementsTab"><fmt:message key="label.complements" /></a>
						</li>
					</c:if>
				</ul>

				<c:if test="${command.allowedOnglet.FISCAL}">
					<div id="tabContent_fiscalTab" class="situation_fiscale">
						<jsp:include page="fiscal/fiscal.jsp" />
					</div>
				</c:if>
				<c:if test="${command.natureTiers != 'DebiteurPrestationImposable'}">
					<div id="tabContent_civilTab" class="visuTiers">
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
					<div id="tabContent_complementsTab" class="editTiers">
						<jsp:include page="complement/complement.jsp" />
					</div>
				</c:if>
			</div>

			<script>
				$(function() {
					$("#tiersCreationTabs").tabs();
				});
			</script>

			<!-- Fin onglets -->

			<!-- Debut Boutons -->
			<c:choose>
				<c:when test="${command.tiers.numero != null}">
					<unireg:RetourButton link="visu.do?id=${command.tiers.numero}" checkIfModified="true"/>
				</c:when>
				<c:when test="${command.numeroCtbAssocie != null}">
					<unireg:RetourButton link="visu.do?id=${command.numeroCtbAssocie}" checkIfModified="true"/>
				</c:when>
				<c:otherwise>
					<unireg:RetourButton link="../tiers/list.do" checkIfModified="true"/>
				</c:otherwise>
			</c:choose>

			<input type="submit" name="save"  value="<fmt:message key="label.bouton.sauver" />"  />			
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
	
	<script>
			<spring:hasBindErrors name="command">
				<c:forEach items="${errors.globalErrors}" var="error">
					<c:if test="${unireg:startsWith(error.code, 'onglet.error')}">
						$('#<spring:message message="${error}"/>').addClass('error');
					</c:if>
				</c:forEach>
			</spring:hasBindErrors>

			// Initialisation de l'observeur du flag 'modifier'
			Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver ce tiers ?';
			Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver le tiers ?";

			function Page_AnnulerTiers(ev) {
				if(!confirm('Voulez-vous vraiment annuler ce tiers ?'))
					return Event.stop(ev);
				return true;
		 	}
	</script>

	</tiles:put>
</tiles:insert>

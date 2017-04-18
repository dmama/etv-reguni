<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="id" value="${param.id}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<c:if test="${command.tiers != null}">
		<tiles:put name="title">
			<fmt:message key="title.edition.dossiers.apparentes" />
		</tiles:put>
	</c:if>

	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/maj-dossier-apparente.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>

	<tiles:put name="body">
		<form:form method="post" id="formEditTiers" name="theForm">
			<input type="hidden"  name="__TARGET__" value="">
			<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
			<div>
			<!-- Debut affichage du tiers -->
			<c:if test="${command.tiers != null}">
				<unireg:nextRowClass reset="1"/>
				<!-- Debut Caracteristiques generales -->
				<c:if test="${command.tiers.numero != null}">
					<jsp:include page="../../../general/tiers.jsp">
						<jsp:param name="page" value="edit" />
						<jsp:param name="path" value="tiersGeneral" />		
					</jsp:include>
				</c:if>
				<!-- Fin Caracteristiques generales -->
			<unireg:setAuth var="autorisations" tiersId="${command.tiersGeneral.numero}"/>
			<c:if test="${autorisations.rapports}">
				<div id="tabContent_dossiersApparentesTab" class="visuTiers">
					<jsp:include page="dossiers-apparentes.jsp" />
				</div>
			</c:if>
			<c:if test="${autorisations.rapportsEtablissements}">
				<div id="tabContent_dossiersApparentesTab" class="visuTiers">
					<jsp:include page="../etablissement/etablissements.jsp" />
				</div>
			</c:if>
			<c:if test="${autorisations.debiteurs}">
				<div id="tabContent_dossiersApparentesTab" class="visuTiers">
					<jsp:include page="../debiteur/debiteur.jsp" />
				</div>
			</c:if>
			</div>
			<!-- Debut Boutons -->
			<c:choose>
				<c:when test="${command.tiers.numero != null}">
					<unireg:RetourButton link="../tiers/visu.do?id=${command.tiers.numero}" checkIfModified="true"/>
				</c:when>
				<c:otherwise>
					<unireg:RetourButton link="../tiers/list.do" checkIfModified="true"/>
				</c:otherwise>
			</c:choose>
		
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
			// Initialisation de l'observeur du flag 'modifier'
			Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver ce tiers ?';
			Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver le tiers ?";
	</script>
	</tiles:put>
</tiles:insert>

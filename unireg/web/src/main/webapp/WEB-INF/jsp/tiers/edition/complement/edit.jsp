<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="id" value="${param.id}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<c:if test="${command.tiers != null}">
		<tiles:put name="title">
			<fmt:message key="title.edition.complement" />
		</tiles:put>
	</c:if>

	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/maj-civil-complement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
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
					<jsp:include page="../../../general/tiers.jsp">
						<jsp:param name="page" value="edit" />
						<jsp:param name="path" value="tiersGeneral" />		
					</jsp:include>
				</c:if>
				<!-- Fin Caracteristiques generales -->
			
				<div>
				<div id="tabContent_complementsTab" class="editTiers" >
					<jsp:include page="complement.jsp" />
				</div>
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

	</script>					
	</tiles:put>
</tiles:insert>

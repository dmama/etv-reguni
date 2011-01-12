<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="id" value="${param.id}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<c:if test="${command.id != null}">
		<tiles:put name="title">
			<fmt:message key="title.edition.tiers" />
		</tiles:put>
	</c:if>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
		<form:form method="post" id="formEditDebiteur" name="theForm">

			<!-- Debut affichage du tiers -->
			<unireg:nextRowClass reset="1"/>
			<!-- Debut Caracteristiques generales -->
			<c:set var="titre"><fmt:message key="caracteristiques.tiers"/></c:set>
			<unireg:bandeauTiers numero="${command.id}" titre="${titre}" showValidation="true" showEvenementsCivils="false" showLinks="false"/>
			<!-- Fin Caracteristiques generales -->

			<div id="tabContent_fiscalTab" class="situation_fiscale">
				<jsp:include page="../fiscal/debiteur.jsp"/>
			</div>

			<!-- Debut Boutons -->
			<c:choose>
				<c:when test="${command.id != null}">
					<input type="submit" name="retourVisualisation" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:return Page_RetourToVisualisation(event || window.event);" />
				</c:when>
				<c:otherwise>
					<input type="submit" name="retourList"  value="<fmt:message key="label.bouton.retour" />" onClick="javascript:return Page_RetourListe(event || window.event);" />
				</c:otherwise>
			</c:choose>
			<input type="submit" name="save"  value="<fmt:message key="label.bouton.sauver" />" />
			
			<!-- Fin Boutons -->
			&nbsp;

		<c:if test="${command.id == null}">
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
			// Initialisation de l''observeur du flag 'modifier'
			Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver ce tiers ?';
			Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver le tiers ?";
	</script>

	</tiles:put>
</tiles:insert>

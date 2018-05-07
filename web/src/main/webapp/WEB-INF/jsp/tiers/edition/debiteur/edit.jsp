<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.tiers" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">
		<form:form method="post" id="formEditDebiteur" name="theForm">

			<!-- Debut affichage du tiers -->
			<unireg:nextRowClass reset="1"/>
			<!-- Debut Caracteristiques generales -->
			<unireg:bandeauTiers numero="${command.id}" showValidation="true" showEvenementsCivils="false" showLinks="false"/>
			<!-- Fin Caracteristiques generales -->

			<jsp:include page="../fiscal/debiteur.jsp"/>

			<!-- Debut Boutons -->
			<div style="margin-top: 0.5em">
				<unireg:RetourButton link="../tiers/visu.do?id=${command.id}" checkIfModified="true"/>
				<input type="submit" name="save"  value="<fmt:message key="label.bouton.sauver" />" />
			</div>
	</form:form>

	<script type="text/javascript" language="Javascript1.3">
			// Initialisation de l''observeur du flag 'modifier'
			Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver ce tiers ?';
			Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver le tiers ?";
	</script>

	</tiles:put>
</tiles:insert>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.mouvement.dossier" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/maj-mouvement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formEditMvt" name="theForm">
		<input type="hidden"  name="__TARGET__" value="">
		<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
		<c:set var="ligneTableau" value="${1}" scope="request" />
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../general/contribuable.jsp">
			<jsp:param name="page" value="mouvement" />
			<jsp:param name="path" value="contribuable" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Mouvement dossier -->
		<jsp:include page="mouvement.jsp"/>
		<!-- Fin Mouvement dossier -->
		<!-- Debut Boutons -->
		<input type="button" name="retourMvt" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:Page_RetourEdition(${command.contribuable.numero});" />
		<input type="submit" name="sauverMvt" value="<fmt:message key="label.bouton.sauver" />" onClick="javascript:return Page_SauverMvt(event || window.event, this);" />
		<!-- Fin Boutons -->
	</form:form>
	<script type="text/javascript" language="Javascript1.3">
			Element.addObserver(window, "load", function() {
				var typeMouvementSelect = E$("type_mouvement");
				selectTypeMouvement(typeMouvementSelect.options[typeMouvementSelect.selectedIndex].value);
			});

			/**
			 * Initialisation de l'observeur du flag 'modifier'
			 */
			Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver cette déclaration d\'impôt ?';
			Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver ?";

			function Page_RetourEdition(numero) {
				var status = true;
				if ( Modifier.isModified)
					status =confirm(Modifier.messageOverConfirmation);
				if ( status)
					document.location.href='edit-contribuable.do?numero=' + numero ;
			}
			 	
		 	function Page_SauverMvt(ev, el) {		 		
				if(!confirm('Voulez-vous vraiment sauver ce mouvement de dossier ?'))
					return Event.stop(ev);
				return true;
		 	}
		 			 	
	</script>	
	</tiles:put>
</tiles:insert>
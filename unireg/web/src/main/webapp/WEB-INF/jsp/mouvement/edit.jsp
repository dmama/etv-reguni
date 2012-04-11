<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.mouvement.dossier" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/maj-mouvement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formEditMvt" name="theForm" className="nouveauMouvement">
		<input type="hidden"  name="__TARGET__" value="">
		<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
		<unireg:nextRowClass reset="1"/>
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
		<input type="button" name="retourMvt" value="<fmt:message key="label.bouton.retour" />" onclick="javascript:Page_RetourEdition(${nouveauMouvement.contribuable.numero});" />
		<input type="submit" name="sauverMvt" value="<fmt:message key="label.bouton.sauver" />" />
		<!-- Fin Boutons -->
	</form:form>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/mouvement.js"/>"></script>
	<script type="text/javascript" language="Javascript1.3">
			$(function() {
				selectTypeMouvement($("#type_mouvement").val());

				/**
				 * Initialisation de l'observeur du flag 'modifier'
				 */
				Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
				Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver ce mouvement de dossier ?';
				Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver ?";
			});

			function Page_RetourEdition(numero) {
				var status = true;
				if ( Modifier.isModified)
					status =confirm(Modifier.messageOverConfirmation);
				if ( status)
					document.location.href='edit-contribuable.do?numero=' + numero ;
			}

	</script>
	</tiles:put>
</tiles:insert>
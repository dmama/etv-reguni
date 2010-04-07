<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="depuisTache" value="${param.depuisTache}" />
<c:set var="action" value="${param.action}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.di" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/maj-di.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formEditDI" name="theForm">
		<input type="hidden"  name="__TARGET__" value="">
		<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
	
		<c:if test="${command.errorMessage == null}">
			<c:set var="ligneTableau" value="${1}" scope="request" />
			<!-- Debut Caracteristiques generales -->
			<jsp:include page="../../general/contribuable.jsp">
				<jsp:param name="page" value="di" />
				<jsp:param name="path" value="contribuable" />
			</jsp:include>
			<!-- Fin Caracteristiques generales -->
			<!-- Debut di -->
			<jsp:include page="di.jsp"/>
			<!-- Fin di -->
		</c:if>
		
		
		<!-- Debut Boutons -->
		<c:if test="${depuisTache == null}">
			<input type="button" name="retourDI" value="<fmt:message key="label.bouton.retour" />" onclick="javascript:Page_RetourEdition(${command.contribuable.numero});" />
		</c:if>
		<c:if test="${depuisTache != null}">
			<input type="button" name="retourDI" value="<fmt:message key="label.bouton.retour" />" onclick="javascript:history.go(-1);" />
			<c:if test="${action != null && action != 'newdi'}">
				<input type="submit" name="maintenir" value="<fmt:message key="label.bouton.maintenir.declaration" />" />
			</c:if>
		</c:if>
		
		<c:if test="${command.id != null }">
			<c:if test="${depuisTache == null}">
				<c:if test="${command.allowedQuittancement}">
					<input type="submit" name="save" value="<fmt:message key="label.bouton.sauver" />" />
				</c:if>
				<c:if test="${command.allowedSommation}">
					<c:if test="${command.sommable}">
						<input type="submit" id="boutonSommerActif" name="sommer" value="<fmt:message key="label.bouton.sommer" />"  onclick="javascript:return Page_SommerDI(event || window.event);" />
						<input type="submit" id="boutonSommerNonActif" style="display:none;" name="sommer" disabled="disabled"  value="<fmt:message key="label.bouton.sommer" />"  onclick="javascript:return Page_SommerDI(event || window.event);" />
					</c:if>
					<c:if test="${command.wasSommee}">
						<input type="submit" name="copierSommation" value="<fmt:message key="label.bouton.copie.conforme.sommation" />"  onclick="javascript:return Page_CopierSommationDI(event || window.event);" />
					</c:if>
					<input type="submit" id="boutonCopieConformeActif" style="display:none;" name="copierSommation" value="<fmt:message key="label.bouton.copie.conforme.sommation" />"  onclick="javascript:return Page_CopierSommationDI(event || window.event);" />
				</c:if>
	
				<!-- Duplicata DI -->
				
				<c:if test="${command.allowedDuplic}">
					<a href="impression.do?id=${command.id}&height=410&width=500&TB_iframe=true&modal=true" class="thickbox"><input type="button" value="<fmt:message key="label.bouton.imprimer.duplicata" />"></a>
				</c:if>
				<!-- Impression de chemise de taxation d'office -->
				<c:if test="${command.allowedSommation}"> 
					<c:if test="${command.etat == 'ECHUE'}">
						<input type="submit" name="imprimerTO" value="<fmt:message key="label.bouton.imprimer.to" />" onclick="javascript:return Page_ImprimerTO(event || window.event);" />
					</c:if>
				</c:if>
			</c:if>		
			<!-- Annulation DI -->
			<c:if test="${command.allowedSommation}">
				<input type="submit" name="annulerDI" value="<fmt:message key="label.bouton.annuler.declaration" />" onclick="javascript:return Page_AnnulerDI(event || window.event);" />
			</c:if>
			
		</c:if>
		<c:if test="${command.id == null}">
			<c:if test="${command.imprimable == false}"> 
				<input type="button" name="imprimerDI" disabled="disabled" value="<fmt:message key="label.bouton.imprimer" />" onclick="javascript:return Page_ImprimerDI(event || window.event, this);" />
			</c:if>
			<c:if test="${command.imprimable == true}"> 
				<input type="button" name="imprimerDI" value="<fmt:message key="label.bouton.imprimer" />" onclick="javascript:return Page_ImprimerDI(event || window.event, this);" />
			</c:if>
		</c:if>
		<!-- Fin Boutons -->
	</form:form>
	<script type="text/javascript" language="Javascript1.3">

			/**
			 * Initialisation de l'observeur du flag 'modifier'
			 */
			Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver cette déclaration d\'impôt ?';
			Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver ?";

			function Page_RetourEdition(numero) {
				var status = true;
				if (document.theForm.imprimable != null) {
					if ( Modifier.isModified && (document.theForm.imprimable.value == true))
						status =confirm(Modifier.messageOverConfirmation);
				}
				if ( status )
					document.location.href='edit.do?action=listdis&numero=' + numero ;
			}
			
			function Page_SommerDI(ev) {
				if(!confirm('Voulez-vous vraiment sommer cette déclaration d\'impôt ?')) {
					return Event.stop(ev);
				}
				Element.hide('boutonSommerActif');
				Element.show('boutonSommerNonActif');
				Element.show('boutonCopieConformeActif');
				return true;
		 	}

			function Page_CopierSommationDI(ev) {
				if(!confirm('Voulez-vous vraiment imprimer cette copie conforme de sommation de déclaration d\'impôt ?'))
					return Event.stop(ev);
				return true;
		 	}
		 	
		 	function Page_AnnulerDI(ev) {
				if(!confirm('Voulez-vous vraiment annuler cette déclaration d\'impôt ?'))
					return Event.stop(ev);
				return true;
		 	}
		 	
		 	function Page_ImprimerDI(ev, el) {		 
				if(!confirm('Voulez-vous vraiment imprimer cette déclaration d\'impôt ?'))
					return Event.stop(ev);
				var form = F$("theForm");
				form.doPostBack("imprimerDI", "");
				el.disabled = true; 
				return true;
		 	}
		 			 	
		 	function Page_ImprimerTO(ev, el) {		 		
				if(!confirm('Voulez-vous vraiment imprimer cette taxation d\'office ?'))
					return Event.stop(ev);
				return true;
		 	}

		 	
			 	
	</script>	
	</tiles:put>
</tiles:insert>
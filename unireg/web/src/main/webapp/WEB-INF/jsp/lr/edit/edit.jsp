<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.lr" /></tiles:put>

	<tiles:put name="body">
	<form:form method="post" id="formEditLR" name="theForm">
		<input type="hidden"  name="__TARGET__" value="">
		<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/debiteur.jsp">
			<jsp:param name="page" value="lr" />
			<jsp:param name="path" value="dpi" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut LR -->
		<jsp:include page="lr.jsp"/>
		<!-- Fin LR -->
		<!-- Debut Boutons -->
		<input type="button" name="retourLR" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:Page_RetourLR(${command.dpi.numero});" />
		<c:if test="${command.id != null }">
			<input 	type="submit" name="duplicataLR" value="<fmt:message key="label.bouton.imprimer.duplicata" />"
					onClick="javascript:return Page_DuplicataLR(event || window.event);" />
			<!-- <input type="submit" name="sommerLR" value="<fmt:message key="label.bouton.sommer" />" onClick="javascript:return Page_SommerLR(event || window.event);" /> -->
			<c:if test="${command.etat == 'EMISE'}">
				<input type="submit" name="annulerLR" value="<fmt:message key="label.bouton.annuler.liste" />" onClick="javascript:return Page_AnnulerLR(event || window.event);" />
			</c:if>
		</c:if>
		<c:if test="${command.id == null }">
			<input 	type="submit" name="imprimerLR" 
					<c:if test="${command.imprimable == false}">  disabled="disabled" </c:if>
					value="<fmt:message key="label.bouton.imprimer" />" onClick="javascript:return Page_ImprimerLR(event || window.event, this);" />
		</c:if>
		<!-- Fin Boutons -->
	</form:form>
	<script type="text/javascript" language="Javascript1.3">
		Modifier.attachObserver( "theForm", <c:out value="${__MODIFIER__}" />);
		Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver cette liste recapitulative ?';
		Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans sauver ?";
			
		function Page_RetourLR(numero) {
			var status = true;
			if (document.theForm.imprimable != null) {
				if ( Modifier.isModified && (document.theForm.imprimable.value == true))
					status =confirm(Modifier.messageOverConfirmation);
			}
			if ( status)
				document.location.href='edit-debiteur.do?numero=' + numero ;
		}

		function Page_AnnulerLR(ev) {
			if(!confirm('Voulez-vous vraiment annuler cette liste recapitulative ?'))
				return Event.stop(ev);
			return true;
	 	}
		
		function Page_ImprimerLR(ev, el) {		 		
			if(!confirm('Voulez-vous vraiment imprimer cette liste recapitulative ?'))
				return Event.stop(ev);
			Form.doPostBack("theForm", "imprimerLR", "");
			el.disabled = true; 
			return true;
	 	}
	 	
	 	function Page_DuplicataLR(ev) {
			if(!confirm('Voulez-vous vraiment imprimer un duplicata de cette liste recapitulative ?'))
				return Event.stop(ev);
			return true;
	 	}
	 		
	 	function Page_SommerLR(ev) {
			if(!confirm('Voulez-vous vraiment sommer cette liste recapitulative ?'))
				return Event.stop(ev);
			return true;
 		}
	</script>
	</tiles:put>
</tiles:insert>

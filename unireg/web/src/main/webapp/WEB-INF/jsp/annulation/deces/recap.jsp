<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.annulation.deces" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/annulation-deces.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">

	  	<form:form method="post" id="formRecapAnnulationDeces"  name="formRecapAnnulationDeces">
			<jsp:include page="../../general/pp.jsp">
				<jsp:param name="page" value="annulationDeces" />
				<jsp:param name="path" value="personne" />
			</jsp:include>
			<fieldset class="information">
				<legend><span><fmt:message key="title.caracteristiques.annulation.deces" /></span></legend>
				<table>
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td width="25%"><fmt:message key="label.date.deces" />&nbsp;:</td>
						<td width="75%"><unireg:regdate regdate="${command.dateDeces}" /></td>
					</tr>
				</table>
			</fieldset>
			<!-- Debut Boutons -->
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:Page_RetourRecapAnnulDeces(${command.personne.numero});" />
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_sauverAnnulDeces(event || window.event);" />	
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_RetourRecapAnnulDeces(numero) {
				if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {
					document.location.href='list.do' ;
				}
			}
			function Page_sauverAnnulDeces(event) {
				if(!confirm('Voulez-vous vraiment annuler le décès de cette personne ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>
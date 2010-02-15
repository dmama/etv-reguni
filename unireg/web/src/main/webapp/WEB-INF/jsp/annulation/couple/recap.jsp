<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.annulation.menage.commun" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/annulation-couple.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">

	  	<form:form method="post" id="formRecapAnnulationCouple"  name="formRecapAnnulationCouple">
			<jsp:include page="../../general/tiers.jsp">
				<jsp:param name="page" value="couple" />
				<jsp:param name="path" value="couple" />
			</jsp:include>
			<fieldset class="information">
				<legend><span><fmt:message key="title.caracteristiques.menage.commun" /></span></legend>
				<table>
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td width="25%"><fmt:message key="label.date.menage.commun" />&nbsp;:</td>
						<td width="75%"><unireg:regdate regdate="${command.dateMenageCommun}" /></td>
					</tr>
				</table>
			</fieldset>
			<!-- Debut Boutons -->
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:Page_RetourRecapSeparation(${command.couple.numero});" />
			<input type="submit" value="<fmt:message key="label.bouton.annuler.menage"/>" onClick="javascript:return Page_sauverSeparation(event || window.event);" />	
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_RetourRecapSeparation(numeroPP1) {
				if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {
					document.location.href='list.do' ;
				}
			}
			function Page_sauverSeparation(event) {
				if(!confirm('Voulez-vous vraiment annuler la mise en ménage commun de ces deux personnes ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>
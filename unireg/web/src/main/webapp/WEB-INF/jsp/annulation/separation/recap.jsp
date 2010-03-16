<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.annulation.separation" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/annulation-separation.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">

	  	<form:form method="post" id="formRecapAnnulationSeparation"  name="formRecapAnnulationSeparation">
			<jsp:include page="../../general/pp.jsp">
				<jsp:param name="page" value="annulationSeparation" />
				<jsp:param name="path" value="premierePersonne" />
			</jsp:include>
			<jsp:include page="../../general/pp.jsp">
				<jsp:param name="page" value="annulationSeparation" />
				<jsp:param name="path" value="secondePersonne" />
			</jsp:include>
			<fieldset class="information">
				<legend><span><fmt:message key="title.caracteristiques.annulation.separation" /></span></legend>
				<table>
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td width="25%"><fmt:message key="label.date.separation" />&nbsp;:</td>
						<td width="75%"><unireg:regdate regdate="${command.dateSeparation}" /></td>
					</tr>
				</table>
			</fieldset>
			<!-- Debut Boutons -->
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:Page_RetourRecapAnnulSeparation(${command.premierePersonne.numero});" />
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_sauverAnnulSeparation(event || window.event);" />	
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_RetourRecapAnnulSeparation(numeroPP1) {
				if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {
					document.location.href='list.do' ;
				}
			}
			function Page_sauverAnnulSeparation(event) {
				if(!confirm('Voulez-vous vraiment annuler la séparation de ces deux personnes ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.annulation" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	</tiles:put>
  	<tiles:put name="body">

	  	<form:form method="post" id="formRecapAnnulation"  name="formRecapAnnulation">
			<jsp:include page="../../general/tiers.jsp">
				<jsp:param name="page" value="activation" />
				<jsp:param name="path" value="tiers" />
			</jsp:include>
			<jsp:include page="remplacement.jsp" />
			<jsp:include page="rapport.jsp" />
			<!-- Debut Boutons -->
			<unireg:RetourButton link="../list.do?activation=annulation" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_SauverAnnulation(event || window.event);" />	
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_RetourRecapAnnulation(numero) {
				if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {
					document.location.href='../list.do?activation=annulation' ;
				}
			}
			function Page_SauverAnnulation(event) {
				if(!confirm('Voulez-vous vraiment confirmer cette annulation ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>
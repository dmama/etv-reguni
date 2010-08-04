<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.deces" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/creation-deces.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">

	  	<form:form method="post" id="formRecapDeces"  name="formRecapDeces">
			<jsp:include page="../../../general/pp.jsp">
				<jsp:param name="page" value="deces" />
				<jsp:param name="path" value="personne" />
			</jsp:include>
			<jsp:include page="rapport.jsp" />
			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_sauverDeces(event || window.event);" />	
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_RetourRecapDeces(numero) {
				if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {
					document.location.href='list.do' ;
				}
			}
			function Page_sauverDeces(event) {
				if(!confirm('Voulez-vous vraiment confirmer ce décès ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>
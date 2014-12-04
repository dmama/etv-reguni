<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.separation" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onclick="ouvrirAide('<c:url value='/docs/creation-separation.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">

	  	<form:form method="post" id="formRecapSeparation"  name="formRecapSeparation">
			<jsp:include page="../../../general/tiers.jsp">
				<jsp:param name="page" value="couple" />
				<jsp:param name="path" value="couple" />
			</jsp:include>
			<jsp:include page="rapport.jsp" />
			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver?"/>
			<input type="button" value="<fmt:message key="label.bouton.sauver"/>" onclick="return Page_sauverSeparation();" />
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_sauverSeparation() {
				if (confirm('Voulez-vous vraiment séparer ces deux personnes ?')) {
					$('#formRecapSeparation').submit();
			 	}
			 	return false;
			}
		</script>
	</tiles:put>
</tiles:insert>
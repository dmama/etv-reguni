<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.lrs.debiteur" /></tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/listes-recapitulatives.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	
	<tiles:put name="body">
	<form:form method="post" id="formEditLR" >
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/debiteur.jsp">
			<jsp:param name="page" value="lr" />
			<jsp:param name="path" value="dpi" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Liste de LRs -->
		<jsp:include page="lrs-debiteur.jsp"/>
		<!-- Fin Liste de LRs -->
		<!-- Debut Bouton -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:retourVisuFromLR(${command.dpi.numero});" />
		<!-- Fin Bouton -->
		&nbsp;
	</form:form>
	<script type="text/javascript" language="Javascript1.3">
		function retourVisuFromLR(numero) {
			document.location.href='../tiers/visu.do?id=' + numero ;
		}
		function SubmitFormEditLR(){
			var formEditLR = F$('formEditLR');
			formEditLR.submit();	
		}
	</script>
	</tiles:put>
</tiles:insert>

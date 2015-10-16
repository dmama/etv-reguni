<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="id" value="${param.id}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.edition.contact.impot.source" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">
		<form:form method="post" id="formRecapContactIS"  name="formRecapContactIS">
			<jsp:include page="../../../general/debiteur.jsp">
				<jsp:param name="page" value="debiteur" />
				<jsp:param name="path" value="debiteur" />
			</jsp:include>
			<jsp:include page="../../../general/contribuable.jsp">
				<jsp:param name="page" value="contribuable" />
				<jsp:param name="path" value="contribuable" />
			</jsp:include>
			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do?numeroDpi=${command.debiteur.numero}"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_sauverContactIS(event || window.event);" />	
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_sauverContactIS(event) {
				if(!confirm('Voulez-vous vraiment confirmer ce contact impôt source ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>
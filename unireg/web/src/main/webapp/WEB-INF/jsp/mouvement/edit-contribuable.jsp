<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.mouvement.contribuable" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/maj-mouvement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<unireg:nextRowClass reset="1"/>
	<!-- Debut Caracteristiques generales -->
	<jsp:include page="../general/contribuable.jsp">
		<jsp:param name="page" value="mouvement" />
		<jsp:param name="path" value="contribuable" />
	</jsp:include>
	<!-- Fin Caracteristiques generales -->
	<!-- Debut Liste de DIs -->
	<jsp:include page="mouvements-contribuable.jsp"/>
	<!-- Fin Liste de DIs -->
	<!-- Debut Bouton -->
	<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:retourVisuFromMvt(${command.contribuable.numero});" />
	<!-- Fin Bouton -->
	&nbsp;
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/mouvement.js"/>"></script>
	</tiles:put>
</tiles:insert>
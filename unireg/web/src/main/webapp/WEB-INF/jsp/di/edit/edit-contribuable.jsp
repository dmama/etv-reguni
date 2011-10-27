<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.di.contribuable" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/maj-di.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/contribuable.jsp">
			<jsp:param name="page" value="di" />
			<jsp:param name="path" value="contribuable" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Liste de DIs -->
		<jsp:include page="dis-contribuable.jsp"/>
		<!-- Fin Liste de DIs -->
		<!-- Debut Bouton -->
		<table>
			<tr><td>
				<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:retourVisuFromDI(${command.contribuable.numero});" />
			</td></tr>
		</table>
		<!-- Fin Bouton -->
		&nbsp;
	</tiles:put>
</tiles:insert>
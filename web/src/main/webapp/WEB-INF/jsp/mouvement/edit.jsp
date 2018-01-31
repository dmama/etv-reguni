<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.mouvement.dossier" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/maj-mouvement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formEditMvt" name="theForm" commandName="nouveauMouvement">
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../general/contribuable.jsp">
			<jsp:param name="page" value="mouvement" />
			<jsp:param name="path" value="contribuable" />
			<jsp:param name="commandName" value="nouveauMouvement" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Mouvement dossier -->
		<jsp:include page="mouvement.jsp"/>
		<!-- Fin Mouvement dossier -->
		<!-- Debut Boutons -->
		<c:set var="labelRetour"><fmt:message key="label.bouton.retour"/></c:set>
		<c:choose>
			<c:when test="${nouveauMouvement.idTache != null}">
				<unireg:buttonTo name="${labelRetour}" action="/tache/list.do" method="get"/>
			</c:when>
			<c:otherwise>
				<unireg:buttonTo name="${labelRetour}" action="/tiers/visu.do" params="{id:${nouveauMouvement.contribuable.numero}}" method="get"/>
			</c:otherwise>
		</c:choose>
		<input type="submit" name="sauverMvt" value="<fmt:message key="label.bouton.sauver" />" />
		<!-- Fin Boutons -->
	</form:form>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/mouvement.js"/>"></script>
	</tiles:put>
</tiles:insert>
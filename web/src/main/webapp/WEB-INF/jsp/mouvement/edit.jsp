<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%--@elvariable id="nouveauMouvement" type="ch.vd.unireg.mouvement.view.MouvementDetailView"--%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.mouvement.dossier" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/maj-mouvement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formEditMvt" name="theForm" modelAttribute="nouveauMouvement">
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<unireg:bandeauTiers numero="${nouveauMouvement.contribuable.numero}" titre="caracteristiques.contribuable" cssClass="information" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false"/>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Mouvement dossier -->
		<jsp:include page="mouvement.jsp"/>
		<!-- Fin Mouvement dossier -->
		<!-- Debut Boutons -->
		<c:choose>
			<c:when test="${nouveauMouvement.idTache != null}">
				<unireg:buttonTo name="label.bouton.retour" action="/tache/list.do" method="get"/>
			</c:when>
			<c:otherwise>
				<unireg:buttonTo name="label.bouton.retour" action="/tiers/visu.do" params="{id:${nouveauMouvement.contribuable.numero}}" method="get"/>
			</c:otherwise>
		</c:choose>
		<input type="submit" name="sauverMvt" value="<fmt:message key="label.bouton.sauver" />" />
		<!-- Fin Boutons -->
	</form:form>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/mouvement.js"/>"></script>
	</tiles:put>
</tiles:insert>
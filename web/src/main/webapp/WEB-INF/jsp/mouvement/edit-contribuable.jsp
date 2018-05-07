<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%--@elvariable id="command" type="ch.vd.unireg.mouvement.view.MouvementListView"--%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.mouvement.contribuable" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/maj-mouvement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
	<tiles:put name="body">
	<unireg:nextRowClass reset="1"/>
	<!-- Debut Caracteristiques generales -->
	<c:set var="titre"><fmt:message key="caracteristiques.contribuable"/></c:set>
	<unireg:bandeauTiers numero="${command.contribuable.numero}" titre="${titre}" cssClass="information" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false"/>
	<!-- Fin Caracteristiques generales -->
	<!-- Debut Liste de DIs -->
	<jsp:include page="mouvements-contribuable.jsp"/>
	<!-- Fin Liste de DIs -->
	<!-- Debut Bouton -->
	<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="retourVisuFromMvt(${command.contribuable.numero});" />
	<!-- Fin Bouton -->
	&nbsp;
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/mouvement.js"/>"></script>
	</tiles:put>
</tiles:insert>
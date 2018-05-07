<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="entreprise" value="${data}"/>
<c:set var="nombreElementsTable" value="10"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.civil" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-civil-complement.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">
		<unireg:bandeauTiers numero="${tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="true"/>

		<unireg:setAuth var="autorisations" tiersId="${tiersId}"/>
		<c:if test="${autorisations.donneesCiviles}">

			<div id="edit-etablissement" class="etablissement">
			<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
			<jsp:include page="../../visualisation/civil/etablissement.jsp">
				<jsp:param name="page" value="edit"/>
				<jsp:param name="data" value="${data}"/>
				<jsp:param name="nombreElementsTable" value="${nombreElementsTable}"/>
			</jsp:include>

			<unireg:buttonTo method="get" action="/tiers/visu.do" params="{id:${tiersId}}" name="label.bouton.retour"/>

		</c:if>
		<c:if test="${!autorisations.donneesCiviles}">
			<span class="error"><fmt:message key="error.tiers.interdit" /></span>
		</c:if>
		</div>
	</tiles:put>
</tiles:insert>

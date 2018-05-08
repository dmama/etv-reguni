<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<c:if test="${typeOperation == 'COPIE'}">
		<c:set var="titre">
			<fmt:message key="title.confirmation.copie.droits.acces"/>
		</c:set>
		<c:set var="labelBouton">
			<fmt:message key="label.bouton.copier"/>
		</c:set>
		<c:set var="action" value="copie.do"/>
	</c:if>
	<c:if test="${typeOperation == 'TRANSFERT'}">
		<c:set var="titre">
			<fmt:message key="title.confirmation.transfert.droits.acces"/>
		</c:set>
		<c:set var="labelBouton">
			<fmt:message key="label.bouton.transferer"/>
		</c:set>
		<c:set var="action" value="transfert.do"/>
	</c:if>

	<tiles:put name="title">${titre}</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/acces-copier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">

	<form:form method="post" id="formEditRestriction" name="theForm">

		<unireg:nextRowClass reset="1"/>

		<jsp:include page="../../general/utilisateur.jsp">
			<jsp:param name="path" value="utilisateurReferenceView" />
			<jsp:param name="titleKey" value="label.caracteristiques.utilisateur.reference" />
		</jsp:include>
		<jsp:include page="../../general/utilisateur.jsp">
			<jsp:param name="path" value="utilisateurDestinationView" />
			<jsp:param name="titleKey" value="label.caracteristiques.utilisateur.destination" />
		</jsp:include>

		<jsp:include page="restrictions.jsp" />

	</form:form>

	<form:form action="${action}" commandName="validationData" method="post">
		<input type="hidden" name="visaOperateurReference" value="${visaOperateurReference}"/>
		<input type="hidden" name="visaOperateurDestination" value="${visaOperateurDestination}"/>
		<input type="button" name="retourRecherche" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../copie-transfert.do';" />
		<input type="submit" name="valider" value="${labelBouton}"/>
	</form:form>
	</tiles:put>
</tiles:insert>
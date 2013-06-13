<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="typeOperation" value="${param.typeOperation}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<c:if test="${typeOperation == 'COPIE'}">
			<fmt:message key="title.confirmation.copie.droits.acces" />
		</c:if>
		<c:if test="${typeOperation == 'TRANSFERT'}">
			<fmt:message key="title.confirmation.transfert.droits.acces" />
		</c:if>
	</tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/acces-copier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formEditRestriction"  name="theForm">
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/utilisateur.jsp">
			<jsp:param name="path" value="utilisateurReferenceView" />
			<jsp:param name="titleKey" value="label.caracteristiques.utilisateur.reference" />
		</jsp:include>
		<jsp:include page="../../general/utilisateur.jsp">
			<jsp:param name="path" value="utilisateurDestinationView" />
			<jsp:param name="titleKey" value="label.caracteristiques.utilisateur.destination" />
		</jsp:include>
		
		<jsp:include page="restrictions.jsp" />
		
		<input type="button" name="retourRecherche" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:document.location.href='select-utilisateurs.do';" />
		<c:if test="${typeOperation == 'COPIE'}">
			<input type="submit" name="copier" value="<fmt:message key="label.bouton.copier" />" />
		</c:if>
		<c:if test="${typeOperation == 'TRANSFERT'}">
			<input type="submit" name="transferer" value="<fmt:message key="label.bouton.transferer" />" />
		</c:if>
	</form:form>
	</tiles:put>
</tiles:insert>